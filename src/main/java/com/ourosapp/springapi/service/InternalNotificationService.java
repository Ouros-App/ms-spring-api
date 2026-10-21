package com.ourosapp.springapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo processamento e ingestão de notificações em lote
 * provenientes de microsserviços internos (ex: Engine de Audiência).
 * Implementa deduplicação estrita via event_id e despacho de Push Notifications via FCM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalNotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushService fcmPushService;
    private final ObjectMapper objectMapper;

    /**
     * Processa um lote de notificações internas com garantia de idempotência e disparo de push.
     *
     * @param request DTO contendo a lista de notificações a serem ingeridas
     * @return DTO com o sumário de processamento (recebidos, criados, ignorados e pushes despachados)
     */
    @Transactional
    public InternalBatchNotificationResponseDTO processBatch(InternalBatchNotificationRequestDTO request) {
        if (request == null || request.notifications() == null || request.notifications().isEmpty()) {
            return new InternalBatchNotificationResponseDTO(0, 0, 0, 0, LocalDateTime.now());
        }

        List<InternalBatchNotificationItemDTO> items = request.notifications();
        int totalReceived = items.size();

        // 1. Extração de event_ids não-nulos para verificação no banco
        Set<String> incomingEventIds = items.stream()
                .map(InternalBatchNotificationItemDTO::eventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> existingEventIds = incomingEventIds.isEmpty()
                ? Collections.emptySet()
                : notificationRepository.findExistingEventIds(incomingEventIds);

        // Conjunto mutável para deduplicação (considera existentes no banco e duplicatas no próprio lote)
        Set<String> processedEventIds = new HashSet<>(existingEventIds);

        List<InternalBatchNotificationItemDTO> itemsToCreate = new ArrayList<>();
        int totalSkipped = 0;

        for (InternalBatchNotificationItemDTO item : items) {
            String eventId = item.eventId();
            if (eventId != null) {
                if (processedEventIds.contains(eventId)) {
                    log.info("Notificação ignorada por deduplicação. event_id={}", eventId);
                    totalSkipped++;
                    continue;
                }
                processedEventIds.add(eventId);
            }
            itemsToCreate.add(item);
        }

        if (itemsToCreate.isEmpty()) {
            log.info("Lote processado: todas as {} notificações foram ignoradas por deduplicação.", totalReceived);
            return new InternalBatchNotificationResponseDTO(totalReceived, 0, totalSkipped, 0, LocalDateTime.now());
        }

        // 2. Mapeamento para entidades Notification e persistência em lote
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notificationsToSave = itemsToCreate.stream()
                .map(item -> Notification.builder()
                        .eventId(item.eventId())
                        .userId(item.userId())
                        .userRole(item.userRole())
                        .title(item.title())
                        .message(item.message())
                        .type(item.type())
                        .idTip(item.idTip())
                        .dataPayload(item.dataPayload())
                        .isRead(false)
                        .createdAt(now)
                        .build())
                .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notificationsToSave);
        int totalCreated = savedNotifications.size();
        log.info("Lote processado com sucesso: {} notificações criadas, {} ignoradas.", totalCreated, totalSkipped);

        // 3. Despacho de Push Notifications (FCM) para os itens com send_push == true
        int totalPushDispatched = dispatchPushes(itemsToCreate);

        return new InternalBatchNotificationResponseDTO(
                totalReceived,
                totalCreated,
                totalSkipped,
                totalPushDispatched,
                LocalDateTime.now()
        );
    }

    private int dispatchPushes(List<InternalBatchNotificationItemDTO> createdItems) {
        List<InternalBatchNotificationItemDTO> pushItems = createdItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.sendPush()))
                .toList();

        if (pushItems.isEmpty()) {
            return 0;
        }

        // Agrupamento por userRole para buscar tokens em lote
        Map<String, Set<Long>> userIdsByRole = pushItems.stream()
                .collect(Collectors.groupingBy(
                        InternalBatchNotificationItemDTO::userRole,
                        Collectors.mapping(InternalBatchNotificationItemDTO::userId, Collectors.toSet())
                ));

        Map<String, List<String>> tokensByUserKey = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : userIdsByRole.entrySet()) {
            String role = entry.getKey();
            Set<Long> userIds = entry.getValue();
            if (!userIds.isEmpty()) {
                List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUserRoleAndUserIdIn(role, userIds);
                for (DeviceToken dt : deviceTokens) {
                    if (dt.getUserId() != null && dt.getToken() != null && !dt.getToken().isBlank()) {
                        String key = buildUserKey(dt.getUserId(), role);
                        tokensByUserKey.computeIfAbsent(key, k -> new ArrayList<>()).add(dt.getToken());
                    }
                }
            }
        }

        int pushDispatchedCount = 0;
        for (InternalBatchNotificationItemDTO item : pushItems) {
            String key = buildUserKey(item.userId(), item.userRole());
            List<String> tokens = tokensByUserKey.getOrDefault(key, Collections.emptyList());

            if (tokens.isEmpty()) {
                log.debug("Nenhum token de dispositivo FCM encontrado para usuário ID {} perfil {}. Push ignorado.",
                        item.userId(), item.userRole());
                continue;
            }

            Map<String, String> payloadMap = parseDataPayload(item.dataPayload(), item.type());
            fcmPushService.sendMulticastPush(tokens, item.title(), item.message(), payloadMap);
            pushDispatchedCount++;
        }

        return pushDispatchedCount;
    }

    private String buildUserKey(Long userId, String userRole) {
        return userId + ":" + (userRole != null ? userRole.toUpperCase() : "");
    }

    private Map<String, String> parseDataPayload(String dataPayload, String type) {
        Map<String, String> result = new HashMap<>();
        if (type != null && !type.isBlank()) {
            result.put("type", type);
        }

        if (dataPayload != null && !dataPayload.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(dataPayload);
                if (root.isObject()) {
                    root.fields().forEachRemaining(entry -> {
                        JsonNode val = entry.getValue();
                        if (val.isValueNode()) {
                            result.put(entry.getKey(), val.asText());
                        } else {
                            result.put(entry.getKey(), val.toString());
                        }
                    });
                } else {
                    result.put("payload", dataPayload);
                }
            } catch (Exception ex) {
                log.warn("Erro ao fazer parse do data_payload para envio push FCM: {}. Usando como texto puro.", ex.getMessage());
                result.put("payload", dataPayload);
            }
        }

        return result;
    }
}

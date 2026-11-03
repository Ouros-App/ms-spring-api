package com.ourosapp.springapi.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.event.LotCreatedEvent;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.service.FcmPushService;
import com.ourosapp.springapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Ouvinte de eventos que reage ao cadastro de novos lotes de aves para disparar
 * notificações In-App e Push Notifications (FCM) aos produtores rurais da fazenda correspondente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotNotificationListener {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");

    private final FarmOwnerRepository farmOwnerRepository;
    private final NotificationService notificationService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushService fcmPushService;
    private final ObjectMapper objectMapper;

    /**
     * Processa o evento de criação de lote de forma assíncrona após o commit da transação.
     *
     * @param event evento contendo os dados do lote recém-criado
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLotCreated(LotCreatedEvent event) {
        if (event == null || event.farmId() == null || event.lotId() == null) {
            return;
        }

        log.info("Processando notificações de criação do Lote ID {} para a fazenda ID {}...",
                event.lotId(), event.farmId());

        try {
            notifyFarmOwners(event);
        } catch (Exception ex) {
            log.error("Erro durante o processamento de notificação do Lote ID {}: {}",
                    event.lotId(), ex.getMessage(), ex);
        }
    }

    private void notifyFarmOwners(LotCreatedEvent event) {
        List<FarmOwner> owners = farmOwnerRepository.findAllByIdFarm(event.farmId());
        if (owners == null || owners.isEmpty()) {
            log.info("Nenhum produtor rural encontrado para a fazenda ID {} ao notificar criação do lote ID {}.",
                    event.farmId(), event.lotId());
            return;
        }

        String farmName = (event.farmName() != null && !event.farmName().isBlank())
                ? event.farmName()
                : "Sua fazenda";

        String title = "Novo Lote Iniciado!";
        String message = buildNotificationMessage(event, farmName);
        Map<String, String> payloadMap = buildPayloadMap(event, farmName);
        String jsonPayload = toJson(payloadMap);

        for (FarmOwner owner : owners) {
            if (owner.getId() == null) {
                continue;
            }

            // 1. Persiste a notificação In-App no banco
            notificationService.createNotification(
                    owner.getId(),
                    RoleConstants.FARM_OWNER,
                    title,
                    message,
                    "LOT_CREATED",
                    jsonPayload
            );

            // 2. Dispara Push Notification via FCM caso possua aparelhos registrados
            List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUserIdAndUserRole(
                    owner.getId(),
                    RoleConstants.FARM_OWNER
            );

            if (deviceTokens != null && !deviceTokens.isEmpty()) {
                List<String> tokens = deviceTokens.stream()
                        .map(DeviceToken::getToken)
                        .filter(Objects::nonNull)
                        .toList();

                if (!tokens.isEmpty()) {
                    fcmPushService.sendMulticastPushNow(tokens, title, message, payloadMap);
                }
            }
        }

        log.info("Notificações de criação do lote ID {} despachadas com sucesso para {} produtor(es) da fazenda ID {}.",
                event.lotId(), owners.size(), event.farmId());
    }

    private String buildNotificationMessage(LotCreatedEvent event, String farmName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Um novo lote (#%d) foi cadastrado para a fazenda %s.", event.lotId(), farmName));

        if (event.receivedChickens() != null && event.receivedChickens() > 0) {
            sb.append(String.format(LOCALE_PT_BR, " Aves recebidas: %,d.", event.receivedChickens()));
        }

        if (event.deliveryDate() != null) {
            sb.append(String.format(" Previsão de entrega: %s.", event.deliveryDate().format(DATE_FORMATTER)));
        }

        return sb.toString();
    }

    private Map<String, String> buildPayloadMap(LotCreatedEvent event, String farmName) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "LOT_CREATED");
        payload.put("lot_id", String.valueOf(event.lotId()));
        payload.put("id_lot", String.valueOf(event.lotId()));
        payload.put("farm_id", String.valueOf(event.farmId()));
        payload.put("id_farm", String.valueOf(event.farmId()));
        payload.put("farm_name", farmName);

        if (event.receivedChickens() != null) {
            payload.put("received_chickens", String.valueOf(event.receivedChickens()));
        }
        if (event.deliveryDate() != null) {
            payload.put("delivery_date", event.deliveryDate().toString());
        }
        if (event.cost() != null) {
            payload.put("cost", String.valueOf(event.cost()));
        }

        return payload;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar dataPayload da notificação de lote ID {}: {}",
                    map.get("lot_id"), e.getMessage());
            return "{}";
        }
    }
}

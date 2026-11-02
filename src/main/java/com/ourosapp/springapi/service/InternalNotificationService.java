package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.NotificationDispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalNotificationService {

    private final NotificationDispatchRepository dispatchRepository;

    @Transactional
    public InternalBatchNotificationResponseDTO processBatch(InternalBatchNotificationRequestDTO request) {
        if (request == null || request.notifications() == null || request.notifications().isEmpty()) {
            return new InternalBatchNotificationResponseDTO(0, 0, 0, 0, LocalDateTime.now());
        }

        List<InternalBatchNotificationItemDTO> items = request.notifications();
        Set<String> processedEventIds = new HashSet<>();
        int totalCreated = 0;
        int totalSkipped = 0;
        int totalPushQueued = 0;
        LocalDateTime now = LocalDateTime.now();

        for (InternalBatchNotificationItemDTO item : items) {
            if (!processedEventIds.add(item.eventId())) {
                totalSkipped++;
                continue;
            }

            boolean sendPush = Boolean.TRUE.equals(item.sendPush());
            Notification notification = Notification.builder()
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
                    .build();

            if (!dispatchRepository.insertNotificationIfAbsent(notification, sendPush)) {
                totalSkipped++;
                continue;
            }

            totalCreated++;
            if (sendPush) {
                totalPushQueued++;
            }
        }

        log.info(
                "Internal notification batch committed: received={}, created={}, skipped={}, push_queued={}",
                items.size(), totalCreated, totalSkipped, totalPushQueued
        );
        return new InternalBatchNotificationResponseDTO(
                items.size(), totalCreated, totalSkipped, totalPushQueued, LocalDateTime.now()
        );
    }
}

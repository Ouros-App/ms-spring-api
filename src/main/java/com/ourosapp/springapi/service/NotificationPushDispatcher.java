package com.ourosapp.springapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.NotificationDispatchRepository;
import com.ourosapp.springapi.repository.NotificationPushOutboxItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.push-dispatcher.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationPushDispatcher {

    private static final int BATCH_SIZE = 100;

    private final NotificationDispatchRepository dispatchRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushService fcmPushService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.notifications.push-dispatcher.fixed-delay-ms:30000}")
    public void dispatchPending() {
        for (NotificationPushOutboxItem item : dispatchRepository.claimPendingBatch(BATCH_SIZE)) {
            try {
                List<String> tokens = deviceTokenRepository
                        .findAllByUserIdAndUserRole(item.userId(), item.userRole()).stream()
                        .map(DeviceToken::getToken)
                        .filter(token -> token != null && !token.isBlank())
                        .distinct()
                        .toList();
                if (tokens.isEmpty()) {
                    dispatchRepository.markNoDevice(item);
                    continue;
                }
                fcmPushService.sendMulticastPushNow(
                        tokens, item.title(), item.message(), parsePayload(item)
                );
                dispatchRepository.markSent(item);
            } catch (Exception ex) {
                Duration delay = Duration.ofSeconds(retryDelaySeconds(item.attemptCount()));
                log.error("Push dispatch failed; notification outbox item {} will retry in {} seconds",
                        item.id(), delay.toSeconds(), ex);
                dispatchRepository.scheduleRetry(item, delay, ex.getMessage());
            }
        }
    }

    static long retryDelaySeconds(int attemptCount) {
        int exponent = Math.max(0, Math.min(attemptCount - 1, 6));
        return Math.min(3600, 60L << exponent);
    }

    private Map<String, String> parsePayload(NotificationPushOutboxItem item) {
        Map<String, String> data = new HashMap<>();
        if (item.dataPayload() != null && !item.dataPayload().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(item.dataPayload());
                if (root.isObject()) {
                    root.fields().forEachRemaining(entry -> {
                        JsonNode value = entry.getValue();
                        data.put(entry.getKey(), value.isValueNode() ? value.asText() : value.toString());
                    });
                } else {
                    data.put("payload", item.dataPayload());
                }
            } catch (Exception ex) {
                log.warn("Invalid notification data payload; forwarding as text (outbox id {})", item.id());
                data.put("payload", item.dataPayload());
            }
        }
        if (item.type() != null && !item.type().isBlank()) {
            data.put("type", item.type());
        }
        if (item.eventId() != null && !item.eventId().isBlank()) {
            data.put("event_id", item.eventId());
        }
        if (item.idTip() != null) {
            data.putIfAbsent("tip_id", item.idTip().toString());
        }
        return data;
    }
}

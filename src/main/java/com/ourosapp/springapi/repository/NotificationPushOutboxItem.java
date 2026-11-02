package com.ourosapp.springapi.repository;

import java.util.UUID;

public record NotificationPushOutboxItem(
        long id,
        UUID claimToken,
        String eventId,
        Long userId,
        String userRole,
        String title,
        String message,
        String type,
        Long idTip,
        String dataPayload,
        int attemptCount
) {
}

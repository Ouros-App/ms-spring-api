package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationDispatchRepository {

    private static final String INSERT_NOTIFICATION = """
            INSERT INTO public.notifications
                (event_id, user_id, user_role, title, message, type, id_tip,
                 data_payload, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?)
            ON CONFLICT (event_id) WHERE event_id IS NOT NULL DO NOTHING
            RETURNING id
            """;

    private static final String CLAIM_BATCH = """
            WITH candidates AS (
                SELECT id
                FROM public.notification_push_outbox
                WHERE (status = 'PENDING' AND available_at <= statement_timestamp())
                   OR (status = 'PROCESSING' AND locked_until <= statement_timestamp())
                ORDER BY available_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            ), claimed AS (
                UPDATE public.notification_push_outbox AS outbox
                SET status = 'PROCESSING',
                    attempt_count = outbox.attempt_count + 1,
                    locked_until = statement_timestamp() + INTERVAL '5 minutes',
                    claim_token = ?
                FROM candidates
                WHERE outbox.id = candidates.id
                RETURNING outbox.id, outbox.notification_id, outbox.claim_token, outbox.attempt_count
            )
            SELECT claimed.id, claimed.claim_token, claimed.attempt_count,
                   notification.event_id, notification.user_id, notification.user_role,
                   notification.title, notification.message, notification.type,
                   notification.id_tip, notification.data_payload
            FROM claimed
            JOIN public.notifications AS notification ON notification.id = claimed.notification_id
            ORDER BY claimed.id
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean insertNotificationIfAbsent(Notification notification, boolean sendPush) {
        List<Long> ids = jdbcTemplate.query(
                INSERT_NOTIFICATION,
                statement -> {
                    statement.setString(1, notification.getEventId());
                    statement.setLong(2, notification.getUserId());
                    statement.setString(3, notification.getUserRole());
                    statement.setString(4, notification.getTitle());
                    statement.setString(5, notification.getMessage());
                    statement.setString(6, notification.getType());
                    statement.setObject(7, notification.getIdTip());
                    statement.setString(8, notification.getDataPayload());
                    statement.setObject(9, notification.getCreatedAt());
                },
                (resultSet, rowNumber) -> resultSet.getLong("id")
        );
        if (ids.isEmpty()) {
            return false;
        }
        if (sendPush) {
            enqueuePush(ids.get(0));
        }
        return true;
    }

    @Transactional
    public void enqueuePush(Long notificationId) {
        jdbcTemplate.update(
                "INSERT INTO public.notification_push_outbox (notification_id) VALUES (?)",
                notificationId
        );
    }

    @Transactional
    public List<NotificationPushOutboxItem> claimPendingBatch(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("claim limit must be positive");
        }
        UUID claimToken = UUID.randomUUID();
        return jdbcTemplate.query(
                CLAIM_BATCH,
                statement -> {
                    statement.setInt(1, limit);
                    statement.setObject(2, claimToken);
                },
                (resultSet, rowNumber) -> {
                    long tipIdValue = resultSet.getLong("id_tip");
                    Long tipId = resultSet.wasNull() ? null : tipIdValue;
                    return new NotificationPushOutboxItem(
                            resultSet.getLong("id"),
                            resultSet.getObject("claim_token", UUID.class),
                            resultSet.getString("event_id"),
                            resultSet.getLong("user_id"),
                            resultSet.getString("user_role"),
                            resultSet.getString("title"),
                            resultSet.getString("message"),
                            resultSet.getString("type"),
                            tipId,
                            resultSet.getString("data_payload"),
                            resultSet.getInt("attempt_count")
                    );
                }
        );
    }

    @Transactional
    public void markSent(NotificationPushOutboxItem item) {
        jdbcTemplate.update(
                """
                UPDATE public.notification_push_outbox
                SET status = 'SENT', locked_until = NULL, claim_token = NULL,
                    sent_at = statement_timestamp(), last_error = NULL
                WHERE id = ? AND claim_token = ? AND status = 'PROCESSING'
                """,
                item.id(), item.claimToken()
        );
    }

    @Transactional
    public void markNoDevice(NotificationPushOutboxItem item) {
        jdbcTemplate.update(
                """
                UPDATE public.notification_push_outbox
                SET status = 'NO_DEVICE', locked_until = NULL, claim_token = NULL,
                    sent_at = statement_timestamp(), last_error = NULL
                WHERE id = ? AND claim_token = ? AND status = 'PROCESSING'
                """,
                item.id(), item.claimToken()
        );
    }

    @Transactional
    public void scheduleRetry(NotificationPushOutboxItem item, Duration delay, String error) {
        long delaySeconds = Math.max(1, delay.toSeconds());
        String safeError = error == null ? "Unknown FCM error" : error.substring(0, Math.min(error.length(), 1000));
        jdbcTemplate.update(
                """
                UPDATE public.notification_push_outbox
                SET status = 'PENDING', available_at = statement_timestamp() + (? * INTERVAL '1 second'),
                    locked_until = NULL, claim_token = NULL, last_error = ?
                WHERE id = ? AND claim_token = ? AND status = 'PROCESSING'
                """,
                delaySeconds, safeError, item.id(), item.claimToken()
        );
    }
}

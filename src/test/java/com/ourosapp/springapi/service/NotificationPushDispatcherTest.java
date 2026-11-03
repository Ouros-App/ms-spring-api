package com.ourosapp.springapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.NotificationDispatchRepository;
import com.ourosapp.springapi.repository.NotificationPushOutboxItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPushDispatcherTest {

    @Mock
    private NotificationDispatchRepository dispatchRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private FcmPushService fcmPushService;

    private NotificationPushDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationPushDispatcher(
                dispatchRepository, deviceTokenRepository, fcmPushService, new ObjectMapper()
        );
    }

    @Test
    void sendsQueuedPushAndMarksItDelivered() {
        NotificationPushOutboxItem item = item();
        when(dispatchRepository.claimPendingBatch(anyInt())).thenReturn(List.of(item));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(10L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of(DeviceToken.builder().token("device-token").build()));

        dispatcher.dispatchPending();

        verify(fcmPushService).sendMulticastPushNow(
                List.of("device-token"), "Dica ativa", "Confira a dica",
                Map.of("type", "TIP_ACTIVATED", "event_id", "audience:tip:42:user:10", "tip_id", "42")
        );
        verify(dispatchRepository).markSent(item);
        verify(dispatchRepository, never()).scheduleRetry(any(), any(), anyString());
    }

    @Test
    void retriesWhenFirebaseFailsInsteadOfDroppingQueuedPush() {
        NotificationPushOutboxItem item = item();
        when(dispatchRepository.claimPendingBatch(anyInt())).thenReturn(List.of(item));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(10L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of(DeviceToken.builder().token("device-token").build()));
        doThrow(new IllegalStateException("Firebase unavailable"))
                .when(fcmPushService).sendMulticastPushNow(anyList(), anyString(), anyString(), anyMap());

        dispatcher.dispatchPending();

        verify(dispatchRepository).scheduleRetry(eq(item), any(Duration.class), contains("Firebase unavailable"));
        verify(dispatchRepository, never()).markSent(any());
    }

    @Test
    void completesWithoutRetryWhenUserHasNoRegisteredDevices() {
        NotificationPushOutboxItem item = item();
        when(dispatchRepository.claimPendingBatch(anyInt())).thenReturn(List.of(item));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(10L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of());

        dispatcher.dispatchPending();

        verify(dispatchRepository).markNoDevice(item);
        verifyNoInteractions(fcmPushService);
    }

    @Test
    void usesCappedExponentialRetryDelay() {
        assertTrue(NotificationPushDispatcher.retryDelaySeconds(1) < NotificationPushDispatcher.retryDelaySeconds(2));
        assertTrue(NotificationPushDispatcher.retryDelaySeconds(20) <= 3600);
    }

    private static NotificationPushOutboxItem item() {
        return new NotificationPushOutboxItem(
                7L,
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "audience:tip:42:user:10",
                10L,
                RoleConstants.FARM_OWNER,
                "Dica ativa",
                "Confira a dica",
                "TIP_ACTIVATED",
                42L,
                "{\"tip_id\":42}",
                1
        );
    }
}

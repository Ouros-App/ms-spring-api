package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.NotificationDispatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalNotificationServiceTest {

    @Mock
    private NotificationDispatchRepository dispatchRepository;

    private InternalNotificationService service;

    @BeforeEach
    void setUp() {
        service = new InternalNotificationService(dispatchRepository);
    }

    @Test
    void persistsBatchAndQueuesRequestedPushes() {
        when(dispatchRepository.insertNotificationIfAbsent(any(Notification.class), anyBoolean())).thenReturn(true);
        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(
                item("event-1", true),
                item("event-2", false)
        ));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(2, response.totalReceived());
        assertEquals(2, response.totalCreated());
        assertEquals(0, response.totalSkipped());
        assertEquals(1, response.totalPushQueued());
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(dispatchRepository, times(2)).insertNotificationIfAbsent(notification.capture(), anyBoolean());
        assertEquals(List.of("event-1", "event-2"), notification.getAllValues().stream().map(Notification::getEventId).toList());
        verify(dispatchRepository).insertNotificationIfAbsent(any(Notification.class), eq(true));
        verify(dispatchRepository).insertNotificationIfAbsent(any(Notification.class), eq(false));
    }

    @Test
    void deduplicatesRepeatedEventIdsWithinOneBatch() {
        when(dispatchRepository.insertNotificationIfAbsent(any(Notification.class), anyBoolean())).thenReturn(true);
        InternalBatchNotificationResponseDTO response = service.processBatch(new InternalBatchNotificationRequestDTO(List.of(
                item("same-event", false),
                item("same-event", true)
        )));

        assertEquals(2, response.totalReceived());
        assertEquals(1, response.totalCreated());
        assertEquals(1, response.totalSkipped());
        assertEquals(0, response.totalPushQueued());
        verify(dispatchRepository, times(1)).insertNotificationIfAbsent(any(Notification.class), anyBoolean());
    }

    @Test
    void countsDatabaseIdempotencyConflictAsSkipped() {
        when(dispatchRepository.insertNotificationIfAbsent(any(Notification.class), anyBoolean())).thenReturn(false);
        InternalBatchNotificationResponseDTO response = service.processBatch(new InternalBatchNotificationRequestDTO(List.of(
                item("already-stored", true)
        )));

        assertEquals(0, response.totalCreated());
        assertEquals(1, response.totalSkipped());
        assertEquals(0, response.totalPushQueued());
    }

    @Test
    void emptyBatchDoesNotTouchPersistence() {
        InternalBatchNotificationResponseDTO response = service.processBatch(
                new InternalBatchNotificationRequestDTO(List.of())
        );

        assertEquals(0, response.totalReceived());
        assertEquals(0, response.totalCreated());
        assertEquals(0, response.totalSkipped());
        verifyNoInteractions(dispatchRepository);
    }

    private static InternalBatchNotificationItemDTO item(String eventId, boolean sendPush) {
        return new InternalBatchNotificationItemDTO(
                eventId, 10L, RoleConstants.FARM_OWNER, "Dica", "Confira a dica",
                "TIP_ACTIVATED", 42L, "{\"tip_id\":42}", sendPush
        );
    }
}

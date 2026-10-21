package com.ourosapp.springapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.entity.Notification;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link InternalNotificationService}.
 */
@ExtendWith(MockitoExtension.class)
class InternalNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private FcmPushService fcmPushService;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> payloadCaptor;

    private ObjectMapper objectMapper;
    private InternalNotificationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new InternalNotificationService(notificationRepository, deviceTokenRepository, fcmPushService, objectMapper);
    }

    @Test
    @DisplayName("Deve processar lote com sucesso, persistir notificações e despachar push FCM")
    void deveProcessarLoteComSucessoEDespacharPush() {
        InternalBatchNotificationItemDTO item1 = new InternalBatchNotificationItemDTO(
                "evt-1", 10L, RoleConstants.FARM_OWNER, "Título 1", "Mensagem 1", "TIP_ACTIVATED", 1L, "{\"farm_id\": 5}", true
        );
        InternalBatchNotificationItemDTO item2 = new InternalBatchNotificationItemDTO(
                "evt-2", 20L, RoleConstants.COMPANY_EMPLOYEE, "Título 2", "Mensagem 2", "TIP_ACTIVATED", null, null, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item1, item2));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken dt1 = DeviceToken.builder().id(1L).userId(10L).userRole(RoleConstants.FARM_OWNER).token("token-farm-1").build();
        DeviceToken dt2 = DeviceToken.builder().id(2L).userId(20L).userRole(RoleConstants.COMPANY_EMPLOYEE).token("token-employee-1").build();

        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.FARM_OWNER), eq(Set.of(10L))))
                .thenReturn(List.of(dt1));
        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.COMPANY_EMPLOYEE), eq(Set.of(20L))))
                .thenReturn(List.of(dt2));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertNotNull(response);
        assertEquals(2, response.totalReceived());
        assertEquals(2, response.totalCreated());
        assertEquals(0, response.totalSkipped());
        assertEquals(2, response.totalPushDispatched());
        assertNotNull(response.processedAt());

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();
        assertEquals(2, saved.size());
        assertEquals("evt-1", saved.get(0).getEventId());
        assertEquals("evt-2", saved.get(1).getEventId());

        verify(fcmPushService, times(2)).sendMulticastPush(anyList(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Deve deduplicar itens com event_id já existente no banco de dados")
    void deveDeduplicarItensJaExistentesNoBanco() {
        InternalBatchNotificationItemDTO itemExistente = new InternalBatchNotificationItemDTO(
                "evt-duplicado", 10L, RoleConstants.FARM_OWNER, "Título Existente", "Mensagem", "TIP_ACTIVATED", null, null, true
        );
        InternalBatchNotificationItemDTO itemNovo = new InternalBatchNotificationItemDTO(
                "evt-novo", 11L, RoleConstants.FARM_OWNER, "Título Novo", "Mensagem Nova", "TIP_ACTIVATED", null, null, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(itemExistente, itemNovo));

        when(notificationRepository.findExistingEventIds(Set.of("evt-duplicado", "evt-novo")))
                .thenReturn(Set.of("evt-duplicado"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken dt = DeviceToken.builder().id(1L).userId(11L).userRole(RoleConstants.FARM_OWNER).token("token-11").build();
        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.FARM_OWNER), eq(Set.of(11L))))
                .thenReturn(List.of(dt));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(2, response.totalReceived());
        assertEquals(1, response.totalCreated());
        assertEquals(1, response.totalSkipped());
        assertEquals(1, response.totalPushDispatched());

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        assertEquals(1, notificationsCaptor.getValue().size());
        assertEquals("evt-novo", notificationsCaptor.getValue().get(0).getEventId());
    }

    @Test
    @DisplayName("Deve deduplicar múltiplos itens com mesmo event_id presentes dentro do mesmo lote")
    void deveDeduplicarItensRepetidosNoMesmoLote() {
        InternalBatchNotificationItemDTO item1 = new InternalBatchNotificationItemDTO(
                "evt-mesmo-lote", 10L, RoleConstants.FARM_OWNER, "Primeiro", "Msg 1", "TIP_ACTIVATED", null, null, false
        );
        InternalBatchNotificationItemDTO item2 = new InternalBatchNotificationItemDTO(
                "evt-mesmo-lote", 10L, RoleConstants.FARM_OWNER, "Segundo (Duplicata)", "Msg 2", "TIP_ACTIVATED", null, null, false
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item1, item2));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(2, response.totalReceived());
        assertEquals(1, response.totalCreated());
        assertEquals(1, response.totalSkipped());
        assertEquals(0, response.totalPushDispatched());

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        assertEquals(1, notificationsCaptor.getValue().size());
        assertEquals("Primeiro", notificationsCaptor.getValue().get(0).getTitle());
    }

    @Test
    @DisplayName("Deve gravar notificação in-app sem disparar push quando send_push for false")
    void deveGravarInAppSemDispararPushQuandoSendPushFalso() {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-resolution", 10L, RoleConstants.FARM_OWNER, "Dica Resolvida", "O consumo voltou ao normal", "TIP_RESOLVED", 2L, null, false
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(1, response.totalReceived());
        assertEquals(1, response.totalCreated());
        assertEquals(0, response.totalSkipped());
        assertEquals(0, response.totalPushDispatched());

        verify(notificationRepository).saveAll(anyList());
        verifyNoInteractions(deviceTokenRepository);
        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("Deve persistir notificação e não falhar quando usuário não possuir tokens cadastrados")
    void deveLidarComUsuariosSemTokensCadastrados() {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-no-tokens", 999L, RoleConstants.FARM_OWNER, "Título", "Mensagem", "TIP_ACTIVATED", null, null, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.FARM_OWNER), eq(Set.of(999L))))
                .thenReturn(Collections.emptyList());

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(1, response.totalReceived());
        assertEquals(1, response.totalCreated());
        assertEquals(0, response.totalSkipped());
        assertEquals(0, response.totalPushDispatched());

        verify(notificationRepository).saveAll(anyList());
        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("Deve retornar todos os contadores zerados quando a requisição for nula ou lista vazia")
    void deveRetornarZerosQuandoRequisicaoNulaOuVazia() {
        InternalBatchNotificationResponseDTO responseNula = service.processBatch(null);
        assertEquals(0, responseNula.totalReceived());
        assertEquals(0, responseNula.totalCreated());

        InternalBatchNotificationResponseDTO responseVazia = service.processBatch(new InternalBatchNotificationRequestDTO(List.of()));
        assertEquals(0, responseVazia.totalReceived());
        assertEquals(0, responseVazia.totalCreated());

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(deviceTokenRepository);
        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("Deve retornar zero criados quando todos os itens já existirem no banco")
    void deveRetornarZeroCriadosQuandoTodosForemDeduplicados() {
        InternalBatchNotificationItemDTO item1 = new InternalBatchNotificationItemDTO(
                "evt-1", 10L, RoleConstants.FARM_OWNER, "T1", "M1", "TIP_ACTIVATED", null, null, true
        );
        InternalBatchNotificationItemDTO item2 = new InternalBatchNotificationItemDTO(
                "evt-2", 20L, RoleConstants.FARM_OWNER, "T2", "M2", "TIP_ACTIVATED", null, null, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item1, item2));

        when(notificationRepository.findExistingEventIds(Set.of("evt-1", "evt-2")))
                .thenReturn(Set.of("evt-1", "evt-2"));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);

        assertEquals(2, response.totalReceived());
        assertEquals(0, response.totalCreated());
        assertEquals(2, response.totalSkipped());
        assertEquals(0, response.totalPushDispatched());

        verify(notificationRepository, never()).saveAll(anyList());
        verifyNoInteractions(deviceTokenRepository);
        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("Deve deserializar data_payload como JSON e repassar campos para push FCM")
    void deveDeserializarDataPayloadParaFCM() {
        String jsonPayload = "{\"farm_id\": 42, \"metric\": \"water\", \"alert\": true}";
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-json", 10L, RoleConstants.FARM_OWNER, "Título", "Mensagem", "TIP_ACTIVATED", null, jsonPayload, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken dt = DeviceToken.builder().id(1L).userId(10L).userRole(RoleConstants.FARM_OWNER).token("token-fcm").build();
        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.FARM_OWNER), eq(Set.of(10L))))
                .thenReturn(List.of(dt));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);
        assertEquals(1, response.totalPushDispatched());

        verify(fcmPushService).sendMulticastPush(
                eq(List.of("token-fcm")),
                eq("Título"),
                eq("Mensagem"),
                payloadCaptor.capture()
        );

        Map<String, String> payload = payloadCaptor.getValue();
        assertEquals("TIP_ACTIVATED", payload.get("type"));
        assertEquals("42", payload.get("farm_id"));
        assertEquals("water", payload.get("metric"));
        assertEquals("true", payload.get("alert"));
    }

    @Test
    @DisplayName("Deve lidar com data_payload com formato de string simples sem quebrar o envio de push")
    void deveLidarComDataPayloadNaoJson() {
        String rawPayload = "not-a-valid-json";
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-raw", 10L, RoleConstants.FARM_OWNER, "Título", "Mensagem", "TIP_ACTIVATED", null, rawPayload, true
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        when(notificationRepository.findExistingEventIds(anyCollection())).thenReturn(Collections.emptySet());
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken dt = DeviceToken.builder().id(1L).userId(10L).userRole(RoleConstants.FARM_OWNER).token("token-fcm").build();
        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.FARM_OWNER), eq(Set.of(10L))))
                .thenReturn(List.of(dt));

        InternalBatchNotificationResponseDTO response = service.processBatch(request);
        assertEquals(1, response.totalPushDispatched());

        verify(fcmPushService).sendMulticastPush(
                eq(List.of("token-fcm")),
                eq("Título"),
                eq("Mensagem"),
                payloadCaptor.capture()
        );

        Map<String, String> payload = payloadCaptor.getValue();
        assertEquals("TIP_ACTIVATED", payload.get("type"));
        assertEquals("not-a-valid-json", payload.get("payload"));
    }
}

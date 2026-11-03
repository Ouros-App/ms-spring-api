package com.ourosapp.springapi.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.event.LotCreatedEvent;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.service.FcmPushService;
import com.ourosapp.springapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link LotNotificationListener}.
 */
@ExtendWith(MockitoExtension.class)
class LotNotificationListenerTest {

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private FcmPushService fcmPushService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LotNotificationListener lotNotificationListener;

    private FarmOwner owner1;
    private FarmOwner owner2;

    @BeforeEach
    void setUp() {
        owner1 = FarmOwner.builder()
                .id(100L)
                .name("Carlos Produtor")
                .email("carlos@fazenda.com")
                .idFarm(10L)
                .build();

        owner2 = FarmOwner.builder()
                .id(101L)
                .name("Maria Produtora")
                .email("maria@fazenda.com")
                .idFarm(10L)
                .build();
    }

    @Test
    @DisplayName("Deve notificar produtores rurais com sucesso (In-App e Push) ao receber evento de lote criado")
    void deveNotificarProdutoresComSucessoAoCriarLote() {
        LotCreatedEvent event = new LotCreatedEvent(
                55L,
                10L,
                "Fazenda Boa Vista",
                1L,
                45000,
                LocalDate.of(2026, 12, 20),
                18500.0,
                RoleConstants.COMPANY_EMPLOYEE,
                200L
        );

        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(owner1));

        DeviceToken token1 = DeviceToken.builder()
                .id(1L)
                .userId(100L)
                .userRole(RoleConstants.FARM_OWNER)
                .token("fcm_token_100")
                .build();

        when(deviceTokenRepository.findAllByUserIdAndUserRole(100L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of(token1));

        lotNotificationListener.onLotCreated(event);

        // Verifica notificação In-App
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(1)).createNotification(
                eq(100L),
                eq(RoleConstants.FARM_OWNER),
                titleCaptor.capture(),
                messageCaptor.capture(),
                typeCaptor.capture(),
                payloadCaptor.capture()
        );

        assertEquals("Novo Lote Iniciado!", titleCaptor.getValue());
        assertTrue(messageCaptor.getValue().contains("#55"));
        assertTrue(messageCaptor.getValue().contains("Fazenda Boa Vista"));
        assertTrue(messageCaptor.getValue().contains("45.000") || messageCaptor.getValue().contains("45000"));
        assertTrue(messageCaptor.getValue().contains("20/12/2026"));
        assertEquals("LOT_CREATED", typeCaptor.getValue());

        String jsonPayload = payloadCaptor.getValue();
        assertTrue(jsonPayload.contains("\"lot_id\":\"55\""));
        assertTrue(jsonPayload.contains("\"farm_id\":\"10\""));
        assertTrue(jsonPayload.contains("\"farm_name\":\"Fazenda Boa Vista\""));

        // Verifica Push Notification FCM
        ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(fcmPushService, times(1)).sendMulticastPushNow(
                tokensCaptor.capture(),
                eq("Novo Lote Iniciado!"),
                eq(messageCaptor.getValue()),
                dataCaptor.capture()
        );

        assertEquals(List.of("fcm_token_100"), tokensCaptor.getValue());
        assertEquals("55", dataCaptor.getValue().get("lot_id"));
        assertEquals("Fazenda Boa Vista", dataCaptor.getValue().get("farm_name"));
        assertEquals("LOT_CREATED", dataCaptor.getValue().get("type"));
    }

    @Test
    @DisplayName("Deve notificar múltiplos produtores da mesma fazenda")
    void deveNotificarMultiplosProdutoresDaMesmaFazenda() {
        LotCreatedEvent event = new LotCreatedEvent(
                77L,
                10L,
                "Fazenda Estrela",
                1L,
                30000,
                LocalDate.of(2026, 11, 30),
                12000.0,
                RoleConstants.COMPANY_EMPLOYEE,
                200L
        );

        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(owner1, owner2));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(anyLong(), eq(RoleConstants.FARM_OWNER)))
                .thenReturn(List.of());

        lotNotificationListener.onLotCreated(event);

        verify(notificationService, times(1)).createNotification(
                eq(100L), eq(RoleConstants.FARM_OWNER), anyString(), anyString(), eq("LOT_CREATED"), anyString()
        );
        verify(notificationService, times(1)).createNotification(
                eq(101L), eq(RoleConstants.FARM_OWNER), anyString(), anyString(), eq("LOT_CREATED"), anyString()
        );
        verify(fcmPushService, never()).sendMulticastPushNow(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve salvar notificação In-App mesmo quando produtor não possui tokens FCM registrados")
    void deveNotificarInAppMesmoSemTokensFcm() {
        LotCreatedEvent event = new LotCreatedEvent(
                88L,
                10L,
                "Fazenda Vale",
                1L,
                20000,
                null,
                8000.0,
                RoleConstants.COMPANY_EMPLOYEE,
                200L
        );

        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(owner1));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(100L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of());

        lotNotificationListener.onLotCreated(event);

        verify(notificationService, times(1)).createNotification(
                eq(100L), eq(RoleConstants.FARM_OWNER), anyString(), anyString(), eq("LOT_CREATED"), anyString()
        );
        verify(fcmPushService, never()).sendMulticastPushNow(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve lidar graciosamente quando fazenda não possui produtores vinculados")
    void deveLidarGraciosamenteSemProdutores() {
        LotCreatedEvent event = new LotCreatedEvent(
                99L,
                999L,
                "Fazenda Fantasma",
                1L,
                10000,
                null,
                5000.0,
                RoleConstants.COMPANY_EMPLOYEE,
                200L
        );

        when(farmOwnerRepository.findAllByIdFarm(999L)).thenReturn(List.of());

        assertDoesNotThrow(() -> lotNotificationListener.onLotCreated(event));

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
        verify(fcmPushService, never()).sendMulticastPushNow(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando nulo ou com identificadores ausentes")
    void deveIgnorarEventoInvalido() {
        assertDoesNotThrow(() -> lotNotificationListener.onLotCreated(null));
        assertDoesNotThrow(() -> lotNotificationListener.onLotCreated(new LotCreatedEvent(null, 10L, "F", 1L, 10, null, 1.0, "ADM", 1L)));
        assertDoesNotThrow(() -> lotNotificationListener.onLotCreated(new LotCreatedEvent(1L, null, "F", 1L, 10, null, 1.0, "ADM", 1L)));

        verify(farmOwnerRepository, never()).findAllByIdFarm(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve capturar exceções durante notificação e registrar log sem quebrar a execução")
    void deveCapturarExcecaoELogarSemPropagar() {
        LotCreatedEvent event = new LotCreatedEvent(
                123L,
                10L,
                "Fazenda Erro",
                1L,
                10000,
                null,
                5000.0,
                RoleConstants.COMPANY_EMPLOYEE,
                200L
        );

        when(farmOwnerRepository.findAllByIdFarm(10L)).thenThrow(new RuntimeException("Simulação de falha no banco"));

        assertDoesNotThrow(() -> lotNotificationListener.onLotCreated(event));
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve formatar mensagem de forma limpa quando dados opcionais forem nulos")
    void deveFormatarMensagemSemDadosOpcionais() {
        LotCreatedEvent event = new LotCreatedEvent(
                10L,
                10L,
                null,
                1L,
                null,
                null,
                null,
                RoleConstants.ADM,
                1L
        );

        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(owner1));
        when(deviceTokenRepository.findAllByUserIdAndUserRole(100L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of());

        lotNotificationListener.onLotCreated(event);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(100L), eq(RoleConstants.FARM_OWNER), anyString(), msgCaptor.capture(), eq("LOT_CREATED"), anyString()
        );

        String msg = msgCaptor.getValue();
        assertTrue(msg.contains("Um novo lote (#10) foi cadastrado para a fazenda Sua fazenda."));
        assertFalse(msg.contains("Aves recebidas"));
        assertFalse(msg.contains("Previsão de entrega"));
    }
}

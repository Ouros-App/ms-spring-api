package com.ourosapp.springapi.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.DeviceToken;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.event.RankingUpdatedEvent;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.DeviceTokenRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.service.FcmPushService;
import com.ourosapp.springapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link RankingNotificationListener}.
 */
@ExtendWith(MockitoExtension.class)
class RankingNotificationListenerTest {

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private FcmPushService fcmPushService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RankingNotificationListener rankingNotificationListener;

    private FarmRankingResponseDTO rank1;
    private FarmRankingResponseDTO rank2;
    private FarmRankingResponseDTO rank3;
    private FarmOwner owner1;
    private CompanyEmployee employee1;

    @BeforeEach
    void setUp() {
        rank1 = new FarmRankingResponseDTO(10L, "Fazenda Ouro", "Sudeste", 1.95, 1L);
        rank2 = new FarmRankingResponseDTO(20L, "Fazenda Prata", "Sul", 1.80, 2L);
        rank3 = new FarmRankingResponseDTO(30L, "Fazenda Bronze", "Centro-Oeste", 1.70, 3L);

        owner1 = FarmOwner.builder()
                .id(100L)
                .name("João da Silva")
                .email("joao@fazenda.com")
                .idFarm(10L)
                .build();

        employee1 = CompanyEmployee.builder()
                .id(200L)
                .name("Carlos Corporativo")
                .email("carlos@empresa.com")
                .idEnterprise(1L)
                .build();
    }

    @Test
    @DisplayName("Deve notificar donos de fazendas e funcionários ao receber evento de ranking atualizado")
    void deveNotificarProdutoresEFuncionariosComSucesso() {
        RankingUpdatedEvent event = new RankingUpdatedEvent(1L, List.of(rank1, rank2, rank3));

        when(farmOwnerRepository.findAllByIdFarmIn(List.of(10L, 20L, 30L)))
                .thenReturn(List.of(owner1));

        DeviceToken ownerToken = DeviceToken.builder()
                .id(1L)
                .userId(100L)
                .userRole(RoleConstants.FARM_OWNER)
                .token("fcm_token_owner")
                .build();

        when(deviceTokenRepository.findAllByUserIdAndUserRole(100L, RoleConstants.FARM_OWNER))
                .thenReturn(List.of(ownerToken));

        when(companyEmployeeRepository.findAllByIdEnterprise(1L))
                .thenReturn(List.of(employee1));

        DeviceToken empToken = DeviceToken.builder()
                .id(2L)
                .userId(200L)
                .userRole(RoleConstants.COMPANY_EMPLOYEE)
                .token("fcm_token_emp")
                .build();

        when(deviceTokenRepository.findAllByUserRoleAndUserIdIn(eq(RoleConstants.COMPANY_EMPLOYEE), eq(List.of(200L))))
                .thenReturn(List.of(empToken));

        rankingNotificationListener.onRankingUpdated(event);

        // Verifica notificação in-app do produtor (1º lugar -> Medalha de Ouro)
        verify(notificationService).createNotification(
                eq(100L),
                eq(RoleConstants.FARM_OWNER),
                eq("Ranking Atualizado!"),
                contains("Medalha de Ouro 🥇"),
                eq("RANKING_UPDATED"),
                anyString()
        );

        // Verifica envio de push FCM para o produtor
        verify(fcmPushService).sendMulticastPush(
                eq(List.of("fcm_token_owner")),
                eq("Ranking Atualizado!"),
                contains("Medalha de Ouro 🥇"),
                anyMap()
        );

        // Verifica notificação in-app do funcionário
        verify(notificationService).createNotification(
                eq(200L),
                eq(RoleConstants.COMPANY_EMPLOYEE),
                eq("Ranking de Fazendas Atualizado"),
                contains("3 granjas participantes"),
                eq("RANKING_UPDATED"),
                anyString()
        );

        // Verifica envio de push FCM para os funcionários
        verify(fcmPushService).sendMulticastPush(
                eq(List.of("fcm_token_emp")),
                eq("Ranking de Fazendas Atualizado"),
                contains("3 granjas participantes"),
                anyMap()
        );
    }

    @Test
    @DisplayName("Deve ignorar processamento graciosamente quando evento for nulo ou lista de rankings vazia")
    void deveIgnorarQuandoEventoOuRankingsVazios() {
        rankingNotificationListener.onRankingUpdated(null);
        rankingNotificationListener.onRankingUpdated(new RankingUpdatedEvent(1L, List.of()));

        verifyNoInteractions(farmOwnerRepository);
        verifyNoInteractions(companyEmployeeRepository);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(fcmPushService);
    }

    @Test
    @DisplayName("Não deve quebrar execução quando ocorrer exceção inesperada")
    void deveTratarExcecaoSemPropagar() {
        RankingUpdatedEvent event = new RankingUpdatedEvent(1L, List.of(rank1));
        when(farmOwnerRepository.findAllByIdFarmIn(anyList()))
                .thenThrow(new RuntimeException("Falha temporária de banco"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                rankingNotificationListener.onRankingUpdated(event)
        );
    }
}

package com.ourosapp.springapi.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.event.RankingUpdatedEvent;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
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

        when(companyEmployeeRepository.findAllByIdEnterprise(1L))
                .thenReturn(List.of(employee1));


        rankingNotificationListener.onRankingUpdated(event);

        verify(notificationService).createNotificationWithPush(
                eq(100L),
                eq(RoleConstants.FARM_OWNER),
                eq("Ranking Atualizado!"),
                contains("Medalha de Ouro 🥇"),
                eq("RANKING_UPDATED"),
                isNull(),
                anyString()
        );
        verify(notificationService).createNotificationWithPush(
                eq(200L),
                eq(RoleConstants.COMPANY_EMPLOYEE),
                eq("Ranking de Fazendas Atualizado"),
                contains("3 granjas participantes"),
                eq("RANKING_UPDATED"),
                isNull(),
                anyString()
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

    @Test
    @DisplayName("Deve cobrir badges de medalhas (Prata, Bronze e sem medalha) e fazenda sem nome")
    void deveCobrirBadgesDeMedalhasEPropriedadesOpcionais() {
        FarmRankingResponseDTO rankPrata = new FarmRankingResponseDTO(20L, null, "Sul", null, 2L);
        FarmRankingResponseDTO rankBronze = new FarmRankingResponseDTO(30L, "Fazenda Bronze", "Centro", 1.5, 3L);
        FarmRankingResponseDTO rankQuarto = new FarmRankingResponseDTO(40L, "Fazenda Quatro", "Norte", 1.2, 4L);

        FarmOwner ownerPrata = FarmOwner.builder().id(101L).idFarm(20L).build();
        FarmOwner ownerBronze = FarmOwner.builder().id(102L).idFarm(30L).build();
        FarmOwner ownerQuarto = FarmOwner.builder().id(103L).idFarm(40L).build();
        FarmOwner ownerInvalido = FarmOwner.builder().id(null).idFarm(20L).build();
        FarmOwner ownerSemFazenda = FarmOwner.builder().id(104L).idFarm(null).build();
        FarmOwner ownerOutraFazenda = FarmOwner.builder().id(105L).idFarm(999L).build();

        RankingUpdatedEvent event = new RankingUpdatedEvent(null, List.of(rankPrata, rankBronze, rankQuarto));

        when(farmOwnerRepository.findAllByIdFarmIn(anyList()))
                .thenReturn(List.of(ownerPrata, ownerBronze, ownerQuarto, ownerInvalido, ownerSemFazenda, ownerOutraFazenda));

        rankingNotificationListener.onRankingUpdated(event);

        verify(notificationService).createNotificationWithPush(
                eq(101L), eq(RoleConstants.FARM_OWNER), anyString(),
                contains("Medalha de Prata 🥈"), anyString(), isNull(), anyString()
        );
        verify(notificationService).createNotificationWithPush(
                eq(102L), eq(RoleConstants.FARM_OWNER), anyString(),
                contains("Medalha de Bronze 🥉"), anyString(), isNull(), anyString()
        );
        verify(notificationService).createNotificationWithPush(
                eq(103L), eq(RoleConstants.FARM_OWNER), anyString(),
                argThat(msg -> msg != null && !msg.contains("Medalha")),
                anyString(), isNull(), anyString()
        );

        // Como enterpriseId é nulo, funcionários não são notificados
        verifyNoInteractions(companyEmployeeRepository);
    }

    @Test
    @DisplayName("Deve ignorar envio quando produtores ou funcionários forem vazios")
    void deveIgnorarQuandoNenhumProdutorOuFuncionarioEncontrado() {
        RankingUpdatedEvent event = new RankingUpdatedEvent(5L, List.of(rank1));

        when(farmOwnerRepository.findAllByIdFarmIn(anyList())).thenReturn(List.of());
        when(companyEmployeeRepository.findAllByIdEnterprise(5L)).thenReturn(List.of(
                CompanyEmployee.builder().id(null).name("Sem ID").build()
        ));

        rankingNotificationListener.onRankingUpdated(event);

        verify(notificationService, never()).createNotificationWithPush(
                anyLong(), anyString(), anyString(), anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    @DisplayName("toJson - Deve retornar fallback JSON vazio quando serialização falhar")
    void deveRetornarJsonVazioEmFalhaDeSerializacao() throws Exception {
        com.fasterxml.jackson.core.JsonProcessingException ex = mock(com.fasterxml.jackson.core.JsonProcessingException.class);
        doThrow(ex).when(objectMapper).writeValueAsString(any());

        RankingUpdatedEvent event = new RankingUpdatedEvent(1L, List.of(rank1));
        when(farmOwnerRepository.findAllByIdFarmIn(anyList())).thenReturn(List.of(owner1));
        when(companyEmployeeRepository.findAllByIdEnterprise(1L)).thenReturn(List.of());

        rankingNotificationListener.onRankingUpdated(event);

        verify(notificationService).createNotificationWithPush(
                eq(100L), anyString(), anyString(), anyString(), anyString(), isNull(), eq("{}")
        );
    }
}

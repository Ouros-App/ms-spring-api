package com.ourosapp.springapi.scheduler;

import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.service.FarmRankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link FarmRankingScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class FarmRankingSchedulerTest {

    @Mock
    private FarmRankingService farmRankingService;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @InjectMocks
    private FarmRankingScheduler farmRankingScheduler;

    @Test
    @DisplayName("Deve executar rotina agendada de ranking com sucesso para todas as empresas")
    void deveExecutarRotinaAgendadaComSucesso() {
        Enterprise enterprise1 = Enterprise.builder().id(1L).name("Empresa 1").build();
        Enterprise enterprise2 = Enterprise.builder().id(2L).name("Empresa 2").build();

        when(enterpriseRepository.findAll()).thenReturn(List.of(enterprise1, enterprise2));
        when(farmRankingService.recalculateAndCacheEnterpriseRanking(1L))
                .thenReturn(List.of(mock(FarmRankingResponseDTO.class)));
        when(farmRankingService.recalculateAndCacheEnterpriseRanking(2L))
                .thenReturn(List.of(mock(FarmRankingResponseDTO.class)));

        farmRankingScheduler.scheduleWeeklyRankingCalculation();

        verify(enterpriseRepository).findAll();
        verify(farmRankingService).recalculateAndCacheEnterpriseRanking(1L);
        verify(farmRankingService).recalculateAndCacheEnterpriseRanking(2L);
    }

    @Test
    @DisplayName("Deve tolerar falha em uma empresa individual sem abortar o restante da rotina")
    void deveTolerarFalhaEmEmpresaIndividual() {
        Enterprise enterprise1 = Enterprise.builder().id(1L).name("Empresa 1").build();
        Enterprise enterprise2 = Enterprise.builder().id(2L).name("Empresa 2").build();

        when(enterpriseRepository.findAll()).thenReturn(List.of(enterprise1, enterprise2));
        when(farmRankingService.recalculateAndCacheEnterpriseRanking(1L))
                .thenThrow(new RuntimeException("Erro temporário de conexão com Redis"));
        when(farmRankingService.recalculateAndCacheEnterpriseRanking(2L))
                .thenReturn(List.of(mock(FarmRankingResponseDTO.class)));

        farmRankingScheduler.scheduleWeeklyRankingCalculation();

        verify(enterpriseRepository).findAll();
        verify(farmRankingService).recalculateAndCacheEnterpriseRanking(1L);
        verify(farmRankingService).recalculateAndCacheEnterpriseRanking(2L);
    }
}

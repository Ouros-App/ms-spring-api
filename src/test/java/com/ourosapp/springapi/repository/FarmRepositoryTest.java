package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.repository.projection.FarmRankingProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para métodos padrão da interface {@link FarmRepository}.
 */
@ExtendWith(MockitoExtension.class)
class FarmRepositoryTest {

    @Test
    @DisplayName("Deve delegar para calculateFarmRankingsByEnterprise quando idEnterprise não for nulo")
    void deveChamarCalculateFarmRankingsByEnterpriseQuandoIdEnterpriseNaoNulo() {
        FarmRepository farmRepository = mock(FarmRepository.class);
        when(farmRepository.calculateFarmRankings(1L)).thenCallRealMethod();
        FarmRankingProjection proj = mock(FarmRankingProjection.class);
        when(farmRepository.calculateFarmRankingsByEnterprise(1L)).thenReturn(List.of(proj));

        List<FarmRankingProjection> result = farmRepository.calculateFarmRankings(1L);

        assertEquals(1, result.size());
        verify(farmRepository).calculateFarmRankingsByEnterprise(1L);
    }

    @Test
    @DisplayName("Deve delegar para calculateAllFarmRankings quando idEnterprise for nulo")
    void deveChamarCalculateAllFarmRankingsQuandoIdEnterpriseForNulo() {
        FarmRepository farmRepository = mock(FarmRepository.class);
        when(farmRepository.calculateFarmRankings(null)).thenCallRealMethod();
        FarmRankingProjection proj = mock(FarmRankingProjection.class);
        when(farmRepository.calculateAllFarmRankings()).thenReturn(List.of(proj));

        List<FarmRankingProjection> result = farmRepository.calculateFarmRankings(null);

        assertEquals(1, result.size());
        verify(farmRepository).calculateAllFarmRankings();
    }
}

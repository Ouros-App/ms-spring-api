package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link FarmRankingService}.
 */
@ExtendWith(MockitoExtension.class)
class FarmRankingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private FarmRankingService farmRankingService;

    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal ownerPrincipal;
    private Farm sampleFarm;

    @BeforeEach
    void setUp() {
        admPrincipal = new UserPrincipal(1L, "adm@ouros.com", "Admin", "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM")));
        employeePrincipal = new UserPrincipal(2L, "emp@ouros.com", "Employee", "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));
        ownerPrincipal = new UserPrincipal(3L, "owner@ouros.com", "Owner", "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));

        sampleFarm = Farm.builder()
                .id(10L)
                .name("Granja Teste")
                .idEnterprise(1L)
                .idAddress(1L)
                .areaProperty(new BigDecimal("100.0"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .chickensNow(45000)
                .build();
    }

    @Test
    @DisplayName("Deve retornar Top Rankings para ADM com sucesso")
    void deveRetornarTopRankingsParaAdm() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(enterpriseRepository.existsById(1L)).thenReturn(true);

        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("10", 95.0));

        when(zSetOperations.reverseRangeWithScores("ranking:farms:enterprise:1", 0, 9))
                .thenReturn(tuples);
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(sampleFarm));

        List<FarmRankingResponseDTO> result = farmRankingService.getTopRankings(1L, 10, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).farmId());
        assertEquals(1L, result.get(0).rankPosition());
        assertEquals(95.0, result.get(0).score());
        assertEquals("Granja Teste", result.get(0).farmName());
    }

    @Test
    @DisplayName("Deve retornar Top Rankings para CompanyEmployee com sucesso")
    void deveRetornarTopRankingsParaFuncionario() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("10", 88.0));

        when(zSetOperations.reverseRangeWithScores("ranking:farms:enterprise:1", 0, 9))
                .thenReturn(tuples);
        when(farmRepository.findAllById(List.of(10L))).thenReturn(List.of(sampleFarm));

        List<FarmRankingResponseDTO> result = farmRankingService.getTopRankings(null, 10, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(88.0, result.get(0).score());
    }

    @Test
    @DisplayName("Deve retornar posição da granja no ranking com sucesso")
    void deveRetornarPosicaoDaGranja() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(zSetOperations.reverseRank("ranking:farms:enterprise:1", "10")).thenReturn(2L);
        when(zSetOperations.score("ranking:farms:enterprise:1", "10")).thenReturn(91.5);

        FarmRankingResponseDTO response = farmRankingService.getFarmPosition(10L, admPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.farmId());
        assertEquals(3L, response.rankPosition()); // 0-based index 2 -> 3º lugar
        assertEquals(91.5, response.score());
    }

    @Test
    @DisplayName("Deve lançar 404 quando fazenda não for encontrada na busca de posição")
    void deveLancar404QuandoFazendaNaoEncontrada() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.getFarmPosition(99L, admPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor tentar consultar fazenda de outro")
    void deveLancar403QuandoProdutorNaoForDono() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        FarmOwner otherOwner = FarmOwner.builder().id(3L).idFarm(99L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(otherOwner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.getFarmPosition(10L, ownerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve atualizar pontuação de ranking com sucesso")
    void deveAtualizarPontuacaoComSucesso() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(zSetOperations.reverseRank("ranking:farms:enterprise:1", "10")).thenReturn(0L);

        FarmScoreUpdateDTO updateDTO = new FarmScoreUpdateDTO(99.0);
        FarmRankingResponseDTO response = farmRankingService.updateFarmScore(10L, updateDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(99.0, response.score());
        assertEquals(1L, response.rankPosition());
        verify(zSetOperations).add("ranking:farms:enterprise:1", "10", 99.0);
        verify(zSetOperations).add("ranking:farms:global", "10", 99.0);
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor rural tentar atualizar pontuação")
    void deveLancar403QuandoProdutorTentarAtualizarScore() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        FarmScoreUpdateDTO updateDTO = new FarmScoreUpdateDTO(99.0);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.updateFarmScore(10L, updateDTO, ownerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve sincronizar fazendas com o Redis com sucesso")
    void deveSincronizarFazendasComSucesso() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(farmRepository.findAll()).thenReturn(List.of(sampleFarm));

        long synced = farmRankingService.syncAllFarmsToRanking(null, admPrincipal);

        assertEquals(1L, synced);
        verify(zSetOperations).add(eq("ranking:farms:enterprise:1"), eq("10"), anyDouble());
    }
}

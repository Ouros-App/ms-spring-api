package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.farm.FarmPodiumItemDTO;
import com.ourosapp.springapi.dto.farm.FarmPodiumRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.projection.FarmRankingProjection;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private HashOperations<String, Object, Object> hashOperations;

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
    private Farm secondFarm;
    private Farm thirdFarm;

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
                .name("Granja Ouro Fino")
                .idEnterprise(1L)
                .idAddress(1L)
                .areaProperty(new BigDecimal("100.0"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .chickensNow(45000)
                .build();

        secondFarm = Farm.builder()
                .id(20L)
                .name("Granja Bela Vista")
                .idEnterprise(1L)
                .idAddress(2L)
                .areaProperty(new BigDecimal("80.0"))
                .region("Sudeste")
                .poultryCapacity(40000)
                .chickensNow(38000)
                .build();

        thirdFarm = Farm.builder()
                .id(30L)
                .name("Granja Esperança")
                .idEnterprise(1L)
                .idAddress(3L)
                .areaProperty(new BigDecimal("120.0"))
                .region("Sudeste")
                .poultryCapacity(60000)
                .chickensNow(52000)
                .build();
    }

    private FarmRankingProjection createMockProjection(Long farmId, String name, Long enterpriseId, Long rankPos, Double cgi, String medal, Double percentile) {
        FarmRankingProjection proj = mock(FarmRankingProjection.class);
        when(proj.getFarmId()).thenReturn(farmId);
        when(proj.getFarmName()).thenReturn(name);
        when(proj.getIdEnterprise()).thenReturn(enterpriseId);
        when(proj.getRankPosition()).thenReturn(rankPos);
        when(proj.getCgi()).thenReturn(cgi);
        when(proj.getCaa()).thenReturn(cgi * 0.7);
        when(proj.getCea()).thenReturn(cgi * 0.3);
        when(proj.getMedal()).thenReturn(medal);
        when(proj.getPercentilePct()).thenReturn(percentile);
        when(proj.getPoultryCapacity()).thenReturn(50000);
        when(proj.getChickensNow()).thenReturn(45000);
        when(proj.getRegion()).thenReturn("Sudeste");
        return proj;
    }

    private Map<Object, Object> createFarmDetailsHash(Long farmId, String name, Long rankPos, Double cgi) {
        Map<Object, Object> map = new HashMap<>();
        map.put("farm_id", farmId.toString());
        map.put("farm_name", name);
        map.put("region", "Sudeste");
        map.put("cgi", String.valueOf(cgi));
        map.put("score", String.valueOf(cgi));
        map.put("rank_position", String.valueOf(rankPos));
        return map;
    }

    @Test
    @DisplayName("Deve retornar Top Rankings para ADM com dados essenciais")
    void deveRetornarTopRankingsParaAdm() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(zSetOperations.zCard("ranking:enterprise:1:zset")).thenReturn(1L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 0, 9)).thenReturn(new LinkedHashSet<>(List.of("10")));

        Map<Object, Object> details = createFarmDetailsHash(10L, "Granja Ouro Fino", 1L, 1.25);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details);

        List<FarmRankingResponseDTO> result = farmRankingService.getTopRankings(1L, 10, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).farmId());
        assertEquals("Granja Ouro Fino", result.get(0).farmName());
        assertEquals("Sudeste", result.get(0).region());
        assertEquals(1.25, result.get(0).score());
        assertEquals(1L, result.get(0).rankPosition());
    }

    @Test
    @DisplayName("Deve retornar Top Rankings para CompanyEmployee com sucesso")
    void deveRetornarTopRankingsParaFuncionario() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(zSetOperations.zCard("ranking:enterprise:1:zset")).thenReturn(1L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 0, 9)).thenReturn(new LinkedHashSet<>(List.of("10")));

        Map<Object, Object> details = createFarmDetailsHash(10L, "Granja Ouro Fino", 1L, 1.25);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details);

        List<FarmRankingResponseDTO> result = farmRankingService.getTopRankings(null, 10, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1.25, result.get(0).score());
        assertEquals("Granja Ouro Fino", result.get(0).farmName());
    }

    @Test
    @DisplayName("Deve sanitizar nome removendo cidade caso venha concatenado")
    void deveSanitizarNomeRemovendoCidade() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(zSetOperations.zCard("ranking:enterprise:1:zset")).thenReturn(1L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 0, 9)).thenReturn(new LinkedHashSet<>(List.of("10")));

        Map<Object, Object> details = createFarmDetailsHash(10L, "Granja Ouro Fino - Uberlândia", 1L, 1.25);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details);

        List<FarmRankingResponseDTO> result = farmRankingService.getTopRankings(1L, 10, admPrincipal);

        assertNotNull(result);
        assertEquals("Granja Ouro Fino - Uberlândia", result.get(0).farmName());
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor tentar consultar ranking completo")
    void deveLancar403QuandoProdutorTentarVerTopRankings() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.getTopRankings(1L, 10, ownerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural não possui permissão"));
    }

    @Test
    @DisplayName("Deve retornar posição individual da granja no ranking com sucesso")
    void deveRetornarPosicaoDaGranja() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(zSetOperations.rank("ranking:enterprise:1:zset", "10")).thenReturn(0L);

        Map<Object, Object> details = createFarmDetailsHash(10L, "Granja Ouro Fino", 1L, 1.25);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details);

        FarmRankingResponseDTO response = farmRankingService.getFarmPosition(10L, admPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.farmId());
        assertEquals("Granja Ouro Fino", response.farmName());
        assertEquals("Sudeste", response.region());
        assertEquals(1.25, response.score());
        assertEquals(1L, response.rankPosition());
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
    @DisplayName("Deve lançar 403 quando produtor tentar consultar posição de fazenda de outro")
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
    @DisplayName("Deve calcular pódio relativo no topo da tabela (1º lugar: CURRENT e BELOW)")
    void deveCalcularPodiumNoTopoDaTabela() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(FarmOwner.builder().id(3L).idFarm(10L).build()));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "10")).thenReturn(0L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 0L, 1L))
                .thenReturn(new LinkedHashSet<>(List.of("10", "20")));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "20")).thenReturn(1L);

        Map<Object, Object> details10 = createFarmDetailsHash(10L, "Granja Ouro Fino", 1L, 1.10);
        Map<Object, Object> details20 = createFarmDetailsHash(20L, "Granja Bela Vista", 2L, 1.30);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details10);
        when(hashOperations.entries("ranking:farm:20:details")).thenReturn(details20);

        FarmPodiumRankingResponseDTO response = farmRankingService.getFarmPodiumRanking(10L, ownerPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.currentFarmId());
        assertEquals(2, response.rankingPodium().size());

        FarmPodiumItemDTO currentItem = response.rankingPodium().get(0);
        assertEquals(10L, currentItem.farmId());
        assertEquals("Granja Ouro Fino", currentItem.farmName());
        assertEquals("Sudeste", currentItem.region());
        assertEquals(1.10, currentItem.score());
        assertEquals(1L, currentItem.rankPosition());
        assertEquals("CURRENT", currentItem.relation());

        FarmPodiumItemDTO belowItem = response.rankingPodium().get(1);
        assertEquals(20L, belowItem.farmId());
        assertEquals("Granja Bela Vista", belowItem.farmName());
        assertEquals("BELOW", belowItem.relation());
        assertEquals(2L, belowItem.rankPosition());
        assertEquals(1.30, belowItem.score());
    }

    @Test
    @DisplayName("Deve calcular pódio relativo no meio da tabela (ABOVE, CURRENT e BELOW)")
    void deveCalcularPodiumNoMeioDaTabela() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(farmRepository.findById(20L)).thenReturn(Optional.of(secondFarm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(FarmOwner.builder().id(3L).idFarm(20L).build()));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "20")).thenReturn(1L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 0L, 2L))
                .thenReturn(new LinkedHashSet<>(List.of("10", "20", "30")));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "10")).thenReturn(0L);
        when(zSetOperations.rank("ranking:enterprise:1:zset", "30")).thenReturn(2L);

        Map<Object, Object> details10 = createFarmDetailsHash(10L, "Granja Ouro Fino", 1L, 1.10);
        Map<Object, Object> details20 = createFarmDetailsHash(20L, "Granja Bela Vista", 2L, 1.30);
        Map<Object, Object> details30 = createFarmDetailsHash(30L, "Granja Esperança", 3L, 1.50);
        when(hashOperations.entries("ranking:farm:10:details")).thenReturn(details10);
        when(hashOperations.entries("ranking:farm:20:details")).thenReturn(details20);
        when(hashOperations.entries("ranking:farm:30:details")).thenReturn(details30);

        FarmPodiumRankingResponseDTO response = farmRankingService.getFarmPodiumRanking(20L, ownerPrincipal);

        assertNotNull(response);
        assertEquals(20L, response.currentFarmId());
        assertEquals(3, response.rankingPodium().size());

        FarmPodiumItemDTO aboveItem = response.rankingPodium().get(0);
        assertEquals(10L, aboveItem.farmId());
        assertEquals("ABOVE", aboveItem.relation());
        assertEquals(1L, aboveItem.rankPosition());

        FarmPodiumItemDTO currentItem = response.rankingPodium().get(1);
        assertEquals(20L, currentItem.farmId());
        assertEquals("CURRENT", currentItem.relation());
        assertEquals(2L, currentItem.rankPosition());

        FarmPodiumItemDTO belowItem = response.rankingPodium().get(2);
        assertEquals(30L, belowItem.farmId());
        assertEquals("BELOW", belowItem.relation());
        assertEquals(3L, belowItem.rankPosition());
    }

    @Test
    @DisplayName("Deve calcular pódio relativo no último lugar (ABOVE e CURRENT)")
    void deveCalcularPodiumNoUltimoLugar() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(farmRepository.findById(30L)).thenReturn(Optional.of(thirdFarm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(FarmOwner.builder().id(3L).idFarm(30L).build()));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "30")).thenReturn(2L);
        when(zSetOperations.range("ranking:enterprise:1:zset", 1L, 3L))
                .thenReturn(new LinkedHashSet<>(List.of("20", "30")));

        when(zSetOperations.rank("ranking:enterprise:1:zset", "20")).thenReturn(1L);

        Map<Object, Object> details20 = createFarmDetailsHash(20L, "Granja Bela Vista", 2L, 1.30);
        Map<Object, Object> details30 = createFarmDetailsHash(30L, "Granja Esperança", 3L, 1.50);
        when(hashOperations.entries("ranking:farm:20:details")).thenReturn(details20);
        when(hashOperations.entries("ranking:farm:30:details")).thenReturn(details30);

        FarmPodiumRankingResponseDTO response = farmRankingService.getFarmPodiumRanking(30L, ownerPrincipal);

        assertNotNull(response);
        assertEquals(30L, response.currentFarmId());
        assertEquals(2, response.rankingPodium().size());

        FarmPodiumItemDTO aboveItem = response.rankingPodium().get(0);
        assertEquals(20L, aboveItem.farmId());
        assertEquals("ABOVE", aboveItem.relation());
        assertEquals(2L, aboveItem.rankPosition());

        FarmPodiumItemDTO currentItem = response.rankingPodium().get(1);
        assertEquals(30L, currentItem.farmId());
        assertEquals("CURRENT", currentItem.relation());
        assertEquals(3L, currentItem.rankPosition());
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor tentar consultar pódio de outra fazenda")
    void deveLancar403QuandoProdutorConsultarPodiumDeOutraFazenda() {
        when(farmRepository.findById(20L)).thenReturn(Optional.of(secondFarm));
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.getFarmPodiumRanking(20L, ownerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Acesso negado a esta fazenda", ex.getReason());
    }

    @Test
    @DisplayName("Deve recalcular e carregar dados no Redis ZSet e Hashes com sucesso")
    void deveRecalcularECarregarRedisComSucesso() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        FarmRankingProjection proj = createMockProjection(10L, "Granja Ouro Fino", 1L, 1L, 1.15, "OURO", 10.0);
        when(farmRepository.calculateFarmRankings(1L)).thenReturn(List.of(proj));

        List<FarmRankingResponseDTO> response = farmRankingService.recalculateAndCacheEnterpriseRanking(1L);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).farmId());
        assertEquals("Granja Ouro Fino", response.get(0).farmName());
        assertEquals("Sudeste", response.get(0).region());
        assertEquals(1.15, response.get(0).score());
        assertEquals(1L, response.get(0).rankPosition());

        verify(redisTemplate).delete("ranking:enterprise:1:zset");
        verify(zSetOperations).add("ranking:enterprise:1:zset", "10", 1.15);
        verify(hashOperations).putAll(eq("ranking:farm:10:details"), anyMap());
        verify(redisTemplate).expire(eq("ranking:enterprise:1:zset"), any(Duration.class));
        verify(redisTemplate).expire(eq("ranking:farm:10:details"), any(Duration.class));
    }

    @Test
    @DisplayName("Deve atualizar pontuação de ranking com sucesso")
    void deveAtualizarPontuacaoComSucesso() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(zSetOperations.rank("ranking:enterprise:1:zset", "10")).thenReturn(0L);

        FarmScoreUpdateDTO updateDTO = new FarmScoreUpdateDTO(1.10);
        FarmRankingResponseDTO response = farmRankingService.updateFarmScore(10L, updateDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(1.10, response.score());
        assertEquals("Granja Ouro Fino", response.farmName());
        assertEquals(1L, response.rankPosition());
        verify(zSetOperations).add("ranking:enterprise:1:zset", "10", 1.10);
        verify(hashOperations).putAll(eq("ranking:farm:10:details"), anyMap());
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor rural tentar atualizar pontuação")
    void deveLancar403QuandoProdutorTentarAtualizarScore() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        FarmScoreUpdateDTO updateDTO = new FarmScoreUpdateDTO(1.10);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmRankingService.updateFarmScore(10L, updateDTO, ownerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve sincronizar fazendas com o Redis com sucesso")
    void deveSincronizarFazendasComSucesso() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        FarmRankingProjection proj = createMockProjection(10L, "Granja Ouro Fino", 1L, 1L, 1.15, "OURO", 10.0);
        when(farmRepository.calculateFarmRankings(null)).thenReturn(List.of(proj));

        long synced = farmRankingService.syncAllFarmsToRanking(null, admPrincipal);

        assertEquals(1L, synced);
        verify(zSetOperations).add(eq("ranking:enterprise:1:zset"), eq("10"), eq(1.15));
    }

    @Test
    @DisplayName("Deve calcular contagem regressiva para próximo reset de domingo com sucesso")
    void deveCalcularContagemRegressivaParaProximoResetComSucesso() {
        farmRankingService.setRankingCron("0 0 0 * * SUN");
        farmRankingService.setRankingZone("America/Sao_Paulo");

        var countdown = farmRankingService.calculateRankingCountdown();

        assertNotNull(countdown);
        assertNotNull(countdown.secondsUntilNextReset());
        assertTrue(countdown.secondsUntilNextReset() > 0);
        assertNotNull(countdown.timeUntilNextReset());
        assertFalse(countdown.timeUntilNextReset().isBlank());
        assertNotNull(countdown.nextResetAt());
        assertTrue(countdown.nextResetAt().contains("T00:00:00"));
    }

    @Test
    @DisplayName("Deve retornar null graciosamente quando expressão cron for inválida")
    void deveRetornarNullSeExpressaoCronForInvalida() {
        farmRankingService.setRankingCron("expressao-invalida");

        var countdown = farmRankingService.calculateRankingCountdown();

        assertNull(countdown);
    }
}

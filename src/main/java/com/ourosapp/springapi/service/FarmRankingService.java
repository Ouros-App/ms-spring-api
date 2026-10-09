package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.farm.FarmPodiumItemDTO;
import com.ourosapp.springapi.dto.farm.FarmPodiumRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.dto.farm.RankingCountdownDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.projection.FarmRankingProjection;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela gestão e consulta de Rankings de Fazendas/Granjas utilizando Redis NoSQL e PostgreSQL.
 * Combina alto desempenho em memória para ordenação com cálculo analítico avançado de indicadores (CAA, CEA, CGI).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FarmRankingService {

    private final StringRedisTemplate redisTemplate;
    private final FarmRepository farmRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    @Value("${app.ranking.cron:0 0 0 */7 * *}")
    private String rankingCron = "0 0 0 */7 * *";

    @Value("${app.ranking.cron-zone:America/Sao_Paulo}")
    private String rankingZone = "America/Sao_Paulo";

    private static final String KEY_ENTERPRISE_ZSET = "ranking:enterprise:%d:zset";
    private static final String KEY_FARM_DETAILS = "ranking:farm:%d:details";
    private static final Duration CACHE_TTL = Duration.ofDays(8);

    /**
     * Recalcula os indicadores de consumo e ranking das fazendas no PostgreSQL e carrega no Redis (ZSet e Hashes).
     *
     * @param enterpriseId identificador opcional da empresa integradora (se nulo, recalcula para todas)
     * @return lista de fazendas com ranking consolidado
     */
    @Transactional(readOnly = true)
    public List<FarmRankingResponseDTO> recalculateAndCacheEnterpriseRanking(Long enterpriseId) {
        List<FarmRankingProjection> projections = farmRepository.calculateFarmRankings(enterpriseId);

        if (projections == null || projections.isEmpty()) {
            log.info("Nenhuma fazenda encontrada para recálculo de ranking da empresa ID: {}", enterpriseId);
            return Collections.emptyList();
        }

        // Agrupa por empresa para atualizar os respectivos Sorted Sets isoladamente
        Map<Long, List<FarmRankingProjection>> groupedByEnterprise = projections.stream()
                .filter(p -> p.getIdEnterprise() != null)
                .collect(Collectors.groupingBy(FarmRankingProjection::getIdEnterprise));

        List<FarmRankingResponseDTO> responseList = new ArrayList<>();
        RankingCountdownDTO countdown = calculateRankingCountdown();

        for (Map.Entry<Long, List<FarmRankingProjection>> entry : groupedByEnterprise.entrySet()) {
            Long currentEnterpriseId = entry.getKey();
            List<FarmRankingProjection> enterpriseProjections = entry.getValue();
            String zsetKey = String.format(KEY_ENTERPRISE_ZSET, currentEnterpriseId);

            // Limpa o ZSet anterior da integradora para recarregar ordenação limpa
            redisTemplate.delete(zsetKey);

            for (FarmRankingProjection p : enterpriseProjections) {
                if (p.getFarmId() == null) {
                    continue;
                }
                Double score = p.getCgi() != null ? p.getCgi() : 0.0;
                redisTemplate.opsForZSet().add(zsetKey, p.getFarmId().toString(), score);

                // Armazena os detalhes no Hash com TTL de 8 dias
                String detailsKey = String.format(KEY_FARM_DETAILS, p.getFarmId());
                Map<String, String> detailsMap = new HashMap<>();
                detailsMap.put("farm_id", String.valueOf(p.getFarmId()));
                detailsMap.put("farm_name", p.getFarmName() != null ? p.getFarmName() : "");
                detailsMap.put("region", p.getRegion() != null ? p.getRegion() : "");
                detailsMap.put("poultry_capacity", String.valueOf(p.getPoultryCapacity() != null ? p.getPoultryCapacity() : 0));
                detailsMap.put("chickens_now", String.valueOf(p.getChickensNow() != null ? p.getChickensNow() : 0));
                detailsMap.put("id_enterprise", String.valueOf(p.getIdEnterprise() != null ? p.getIdEnterprise() : 0));
                detailsMap.put("caa", String.valueOf(p.getCaa() != null ? p.getCaa() : 0.0));
                detailsMap.put("cea", String.valueOf(p.getCea() != null ? p.getCea() : 0.0));
                detailsMap.put("cgi", String.valueOf(score));
                detailsMap.put("score", String.valueOf(score));
                detailsMap.put("rank_position", String.valueOf(p.getRankPosition() != null ? p.getRankPosition() : 1));
                detailsMap.put("percentile_pct", String.valueOf(p.getPercentilePct() != null ? p.getPercentilePct() : 0.0));
                detailsMap.put("medal", p.getMedal() != null ? p.getMedal() : "FERRO");

                redisTemplate.opsForHash().putAll(detailsKey, detailsMap);
                redisTemplate.expire(detailsKey, CACHE_TTL);

                responseList.add(FarmRankingResponseDTO.fromProjection(p).withCountdown(countdown));
            }

            redisTemplate.expire(zsetKey, CACHE_TTL);
        }

        log.info("Recalculado e sincronizado ranking com sucesso para {} fazendas no Redis", responseList.size());
        return responseList;
    }

    /**
     * Retorna a visualização de pódio relativo de uma fazenda (concorrente acima, atual, concorrente abaixo).
     *
     * @param farmId    identificador da fazenda consultada
     * @param principal usuário logado
     * @return DTO com o trio de concorrência direta
     */
    @Transactional(readOnly = true)
    public FarmPodiumRankingResponseDTO getFarmPodiumRanking(Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal);

        String zsetKey = String.format(KEY_ENTERPRISE_ZSET, farm.getIdEnterprise());
        Long zeroBasedRank = redisTemplate.opsForZSet().rank(zsetKey, farmId.toString());

        if (zeroBasedRank == null) {
            // Cache miss: executa recálculo sob demanda
            recalculateAndCacheEnterpriseRanking(farm.getIdEnterprise());
            zeroBasedRank = redisTemplate.opsForZSet().rank(zsetKey, farmId.toString());
        }

        if (zeroBasedRank == null) {
            // Fallback caso a fazenda não tenha registros gerados ainda
            return new FarmPodiumRankingResponseDTO(farmId, List.of(
                    new FarmPodiumItemDTO(farmId, farm.getName(), farm.getRegion(), 0.0, 1L, "CURRENT")
            ));
        }

        long start = Math.max(0, zeroBasedRank - 1);
        long end = zeroBasedRank + 1;
        Set<String> memberIds = redisTemplate.opsForZSet().range(zsetKey, start, end);

        if (memberIds == null || memberIds.isEmpty()) {
            return new FarmPodiumRankingResponseDTO(farmId, List.of(
                    new FarmPodiumItemDTO(farmId, farm.getName(), farm.getRegion(), 0.0, zeroBasedRank + 1, "CURRENT")
            ));
        }

        List<FarmPodiumItemDTO> podiumItems = new ArrayList<>();
        for (String memberIdStr : memberIds) {
            Long memberFarmId = Long.valueOf(memberIdStr);
            Long memberRankZeroBased = redisTemplate.opsForZSet().rank(zsetKey, memberIdStr);
            Long memberRankPos = (memberRankZeroBased != null) ? (memberRankZeroBased + 1) : 1L;

            String relation;
            if (memberFarmId.equals(farmId)) {
                relation = "CURRENT";
            } else if (memberRankZeroBased != null && memberRankZeroBased < zeroBasedRank) {
                relation = "ABOVE";
            } else {
                relation = "BELOW";
            }

            String detailsKey = String.format(KEY_FARM_DETAILS, memberFarmId);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(detailsKey);
            FarmPodiumItemDTO item = mapHashToPodiumItem(memberFarmId, relation, memberRankPos, entries);
            podiumItems.add(item);
        }

        // Ordena por posição ordinal crescente (1º lugar, 2º lugar, ...)
        podiumItems.sort(Comparator.comparing(FarmPodiumItemDTO::rankPosition));

        RankingCountdownDTO countdown = calculateRankingCountdown();
        return new FarmPodiumRankingResponseDTO(farmId, podiumItems).withCountdown(countdown);
    }

    /**
     * Consulta as granjas mais bem posicionadas no ranking da empresa integradora do usuário.
     *
     * @param enterpriseId identificador opcional da empresa integradora (apenas para ADM)
     * @param limit        quantidade máxima de granjas no retorno
     * @param principal    dados do usuário autenticado
     * @return lista ordenada contendo o ranking das granjas
     */
    @Transactional(readOnly = true)
    public List<FarmRankingResponseDTO> getTopRankings(Long enterpriseId, int limit, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (FARM_OWNER.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para visualizar o ranking completo da empresa. Utilize a consulta de pódio da sua fazenda.");
        }

        Long targetEnterpriseId = resolveTargetEnterpriseId(enterpriseId, principal);

        if (targetEnterpriseId == null) {
            // Consulta global (ADM sem especificar empresa): recalcula e retorna ordenado
            return recalculateAndCacheEnterpriseRanking(null);
        }

        String zsetKey = String.format(KEY_ENTERPRISE_ZSET, targetEnterpriseId);
        Long count = redisTemplate.opsForZSet().zCard(zsetKey);

        if (count == null || count == 0) {
            recalculateAndCacheEnterpriseRanking(targetEnterpriseId);
        }

        int safeLimit = (limit <= 0) ? 10 : Math.min(limit, 100);
        Set<String> memberIds = redisTemplate.opsForZSet().range(zsetKey, 0, safeLimit - 1);

        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FarmRankingResponseDTO> result = new ArrayList<>();
        RankingCountdownDTO countdown = calculateRankingCountdown();

        for (String memberIdStr : memberIds) {
            Long farmId = Long.valueOf(memberIdStr);
            String detailsKey = String.format(KEY_FARM_DETAILS, farmId);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(detailsKey);
            FarmRankingResponseDTO dto = mapHashToRankingResponse(farmId, entries);

            if (dto != null) {
                result.add(dto.withCountdown(countdown));
            } else {
                Farm farm = farmRepository.findById(farmId).orElse(null);
                Double score = redisTemplate.opsForZSet().score(zsetKey, memberIdStr);
                Long rankPos = redisTemplate.opsForZSet().rank(zsetKey, memberIdStr);
                result.add(new FarmRankingResponseDTO(
                        farmId,
                        farm != null ? farm.getName() : "Granja #" + farmId,
                        farm != null ? farm.getRegion() : null,
                        score != null ? score : 0.0,
                        rankPos != null ? rankPos + 1 : 1L
                ).withCountdown(countdown));
            }
        }

        return result;
    }

    /**
     * Retorna o ranking completo das fazendas de uma integradora (sem limitação curta).
     *
     * @param requestedEnterpriseId empresa solicitada (opcional para ADM)
     * @param principal             usuário logado
     * @return lista completa das fazendas ranqueadas
     */
    @Transactional(readOnly = true)
    public List<FarmRankingResponseDTO> getEnterpriseRankings(Long requestedEnterpriseId, UserPrincipal principal) {
        return getTopRankings(requestedEnterpriseId, 1000, principal);
    }

    /**
     * Consulta a posição e métricas individuais de uma fazenda específica no ranking.
     *
     * @param farmId    identificador da fazenda
     * @param principal dados do usuário autenticado
     * @return DTO com a classificação da fazenda
     */
    @Transactional(readOnly = true)
    public FarmRankingResponseDTO getFarmPosition(Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal);

        String zsetKey = String.format(KEY_ENTERPRISE_ZSET, farm.getIdEnterprise());
        Long zeroBasedRank = redisTemplate.opsForZSet().rank(zsetKey, farmId.toString());

        if (zeroBasedRank == null) {
            recalculateAndCacheEnterpriseRanking(farm.getIdEnterprise());
            zeroBasedRank = redisTemplate.opsForZSet().rank(zsetKey, farmId.toString());
        }

        String detailsKey = String.format(KEY_FARM_DETAILS, farmId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(detailsKey);
        FarmRankingResponseDTO dto = mapHashToRankingResponse(farmId, entries);

        RankingCountdownDTO countdown = calculateRankingCountdown();
        if (dto != null) {
            return dto.withCountdown(countdown);
        }

        Double score = redisTemplate.opsForZSet().score(zsetKey, farmId.toString());
        Long rankPosition = (zeroBasedRank != null) ? (zeroBasedRank + 1) : 1L;
        Double finalScore = (score != null) ? score : 0.0;

        return new FarmRankingResponseDTO(
                farm.getId(),
                farm.getName(),
                farm.getRegion(),
                finalScore,
                rankPosition
        ).withCountdown(countdown);
    }

    /**
     * Atualiza a pontuação de uma fazenda específica no Redis NoSQL.
     *
     * @param farmId    identificador da fazenda
     * @param dto       novo score a ser gravado
     * @param principal dados do usuário autenticado
     * @return DTO atualizado da fazenda no ranking
     */
    @Transactional(readOnly = true)
    public FarmRankingResponseDTO updateFarmScore(Long farmId, FarmScoreUpdateDTO dto, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Objects.requireNonNull(dto, "Payload de atualização de score não pode ser nulo");
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmMutationPermission(farm, principal, "atualizar score de ranking de");

        String enterpriseKey = String.format(KEY_ENTERPRISE_ZSET, farm.getIdEnterprise());
        redisTemplate.opsForZSet().add(enterpriseKey, farmId.toString(), dto.score());

        Long zeroBasedRank = redisTemplate.opsForZSet().rank(enterpriseKey, farmId.toString());
        Long rankPosition = (zeroBasedRank != null) ? (zeroBasedRank + 1) : 1L;

        String detailsKey = String.format(KEY_FARM_DETAILS, farmId);
        Map<String, String> detailsMap = new HashMap<>();
        detailsMap.put("farm_id", farmId.toString());
        detailsMap.put("farm_name", farm.getName());
        detailsMap.put("score", dto.score().toString());
        detailsMap.put("cgi", dto.score().toString());
        detailsMap.put("rank_position", rankPosition.toString());
        redisTemplate.opsForHash().putAll(detailsKey, detailsMap);

        RankingCountdownDTO countdown = calculateRankingCountdown();
        return new FarmRankingResponseDTO(
                farm.getId(),
                farm.getName(),
                farm.getRegion(),
                dto.score(),
                rankPosition
        ).withCountdown(countdown);
    }

    /**
     * Recalcula e sincroniza todas as fazendas de uma empresa integradora a partir do PostgreSQL para o Redis.
     *
     * @param enterpriseId identificador da empresa integradora (opcional para ADM)
     * @param principal    dados do usuário autenticado
     * @return quantidade de fazendas sincronizadas no Redis
     */
    @Transactional(readOnly = true)
    public long syncAllFarmsToRanking(Long enterpriseId, UserPrincipal principal) {
        ensureAuthenticated(principal);
        if (FARM_OWNER.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para executar sincronização de ranking");
        }
        Long targetEnterpriseId = resolveTargetEnterpriseId(enterpriseId, principal);
        List<FarmRankingResponseDTO> synced = recalculateAndCacheEnterpriseRanking(targetEnterpriseId);
        return synced.size();
    }

    // ==========================================
    // Métodos Auxiliares de Mapeamento
    // ==========================================

    private FarmRankingResponseDTO mapHashToRankingResponse(Long farmId, Map<Object, Object> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String name = (String) entries.get("farm_name");
        String region = (String) entries.get("region");
        Double score = parseDouble(entries.get("score"));
        Double cgi = parseDouble(entries.get("cgi"));
        Long rankPos = parseLong(entries.get("rank_position"));

        Double finalScore = score != null ? score : (cgi != null ? cgi : 0.0);

        return new FarmRankingResponseDTO(
                farmId,
                name,
                region,
                finalScore,
                rankPos != null ? rankPos : 1L
        );
    }

    private FarmPodiumItemDTO mapHashToPodiumItem(Long farmId, String relation, Long rankPos, Map<Object, Object> entries) {
        String name = (entries != null && entries.get("farm_name") != null) ? (String) entries.get("farm_name") : "Granja #" + farmId;
        String region = (entries != null && entries.get("region") != null) ? (String) entries.get("region") : null;
        Double score = (entries != null && entries.get("score") != null) ? parseDouble(entries.get("score"))
                : (entries != null && entries.get("cgi") != null ? parseDouble(entries.get("cgi")) : 0.0);
        Long finalRankPos = (rankPos != null) ? rankPos : (entries != null ? parseLong(entries.get("rank_position")) : null);

        return new FarmPodiumItemDTO(farmId, name, region, score, finalRankPos != null ? finalRankPos : 1L, relation);
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==========================================
    // Métodos Auxiliares de Permissão e Validação
    // ==========================================

    private Long resolveTargetEnterpriseId(Long requestedEnterpriseId, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            if (requestedEnterpriseId != null && !enterpriseRepository.existsById(requestedEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa integradora não encontrada para o ID: " + requestedEnterpriseId);
            }
            return requestedEnterpriseId;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            if (requestedEnterpriseId != null && !Objects.equals(employee.getIdEnterprise(), requestedEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para consultar dados de outra integradora");
            }
            return employee.getIdEnterprise();
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado"));
            Farm farm = findFarmByIdOrThrow(owner.getIdFarm());
            return farm.getIdEnterprise();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para acessar ranking");
    }

    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta fazenda");
            }
            return;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado"));
            if (!Objects.equals(farm.getId(), owner.getIdFarm())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta fazenda");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para acessar esta fazenda");
    }

    private void validateFarmMutationPermission(Farm farm, UserPrincipal principal, String action) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action + " esta fazenda");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para alterar ranking de fazendas");
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private Farm findFarmByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: " + id));
    }

    /**
     * Calcula dinamicamente o tempo restante até o próximo recálculo/reset do ranking com base na expressão cron.
     *
     * @return DTO com segundos restantes, texto amigável e data/hora exata ISO-8601, ou null se não for possível calcular
     */
    public RankingCountdownDTO calculateRankingCountdown() {
        try {
            CronExpression cronExpression = CronExpression.parse(rankingCron);
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(rankingZone != null ? rankingZone : "America/Sao_Paulo");
            } catch (Exception e) {
                zoneId = ZoneId.of("America/Sao_Paulo");
            }
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime nextExecution = cronExpression.next(now);

            if (nextExecution == null) {
                return null;
            }

            Duration duration = Duration.between(now, nextExecution);
            long totalSeconds = Math.max(0, duration.getSeconds());

            String humanReadable = formatDurationHuman(duration);
            String nextResetAtIso = nextExecution.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            return new RankingCountdownDTO(totalSeconds, humanReadable, nextResetAtIso);
        } catch (Exception e) {
            log.warn("Não foi possível calcular a contagem regressiva para o cron '{}': {}", rankingCron, e.getMessage());
            return null;
        }
    }

    private String formatDurationHuman(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days == 1 ? "1 dia" : days + " dias");
        }
        if (hours > 0) {
            parts.add(hours == 1 ? "1 hora" : hours + " horas");
        }
        if (minutes > 0) {
            parts.add(minutes == 1 ? "1 minuto" : minutes + " minutos");
        }
        if (parts.isEmpty()) {
            parts.add(seconds == 1 ? "1 segundo" : seconds + " segundos");
        }

        if (parts.size() == 1) {
            return parts.get(0);
        } else if (parts.size() == 2) {
            return parts.get(0) + " e " + parts.get(1);
        } else {
            return parts.get(0) + ", " + parts.get(1) + " e " + parts.get(2);
        }
    }

    public void setRankingCron(String rankingCron) {
        this.rankingCron = rankingCron;
    }

    public void setRankingZone(String rankingZone) {
        this.rankingZone = rankingZone;
    }
}

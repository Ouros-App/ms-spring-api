package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela gestão de Rankings de Fazendas/Granjas utilizando Redis NoSQL (Sorted Sets).
 * Combina alto desempenho em memória para ordenação com enriquecimento de dados a partir do PostgreSQL.
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

    private static final String KEY_PREFIX = "ranking:farms:enterprise:";
    private static final String KEY_GLOBAL = "ranking:farms:global";

    /**
     * Consulta as granjas mais bem posicionadas no ranking (Top N) para a empresa integradora do usuário autenticado.
     *
     * @param enterpriseId identificador opcional da empresa integradora (apenas para ADM)
     * @param limit        quantidade máxima de granjas no retorno
     * @param principal    dados do usuário autenticado
     * @return lista ordenada contendo o ranking das granjas
     */
    @Transactional(readOnly = true)
    public List<FarmRankingResponseDTO> getTopRankings(Long enterpriseId, int limit, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Long targetEnterpriseId = resolveTargetEnterpriseId(enterpriseId, principal);

        String key = (targetEnterpriseId != null) ? (KEY_PREFIX + targetEnterpriseId) : KEY_GLOBAL;

        int safeLimit = (limit <= 0) ? 10 : Math.min(limit, 100);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, safeLimit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> farmIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .toList();

        Map<Long, Farm> farmMap = farmRepository.findAllById(farmIds).stream()
                .collect(Collectors.toMap(Farm::getId, Function.identity()));

        List<FarmRankingResponseDTO> result = new ArrayList<>();
        long position = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null) {
                continue;
            }
            Long farmId = Long.valueOf(tuple.getValue());
            Double score = (tuple.getScore() != null) ? tuple.getScore() : 0.0;
            Farm farm = farmMap.get(farmId);

            String name = (farm != null) ? farm.getName() : "Granja #" + farmId;
            String region = (farm != null) ? farm.getRegion() : null;
            Integer capacity = (farm != null) ? farm.getPoultryCapacity() : null;
            Integer chickens = (farm != null) ? farm.getChickensNow() : null;

            result.add(new FarmRankingResponseDTO(farmId, position++, score, name, region, capacity, chickens));
        }

        return result;
    }

    /**
     * Consulta a posição e o score individual de uma fazenda específica no ranking.
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

        String key = KEY_PREFIX + farm.getIdEnterprise();
        Long zeroBasedRank = redisTemplate.opsForZSet().reverseRank(key, farmId.toString());
        Double score = redisTemplate.opsForZSet().score(key, farmId.toString());

        Long rankPosition = (zeroBasedRank != null) ? (zeroBasedRank + 1) : null;
        Double finalScore = (score != null) ? score : 0.0;

        return new FarmRankingResponseDTO(
                farm.getId(),
                rankPosition,
                finalScore,
                farm.getName(),
                farm.getRegion(),
                farm.getPoultryCapacity(),
                farm.getChickensNow()
        );
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

        String enterpriseKey = KEY_PREFIX + farm.getIdEnterprise();
        redisTemplate.opsForZSet().add(enterpriseKey, farmId.toString(), dto.score());
        redisTemplate.opsForZSet().add(KEY_GLOBAL, farmId.toString(), dto.score());

        Long zeroBasedRank = redisTemplate.opsForZSet().reverseRank(enterpriseKey, farmId.toString());
        Long rankPosition = (zeroBasedRank != null) ? (zeroBasedRank + 1) : 1L;

        return new FarmRankingResponseDTO(
                farm.getId(),
                rankPosition,
                dto.score(),
                farm.getName(),
                farm.getRegion(),
                farm.getPoultryCapacity(),
                farm.getChickensNow()
        );
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
        Long targetEnterpriseId = resolveTargetEnterpriseId(enterpriseId, principal);

        List<Farm> farms;
        if (targetEnterpriseId != null) {
            farms = farmRepository.findAll().stream()
                    .filter(f -> Objects.equals(f.getIdEnterprise(), targetEnterpriseId))
                    .toList();
        } else {
            farms = farmRepository.findAll();
        }

        long count = 0;
        for (Farm farm : farms) {
            if (farm.getId() == null || farm.getIdEnterprise() == null) {
                continue;
            }
            // Cálculo base de score: percentual de ocupação ou capacidade
            double score = 0.0;
            if (farm.getPoultryCapacity() != null && farm.getPoultryCapacity() > 0 && farm.getChickensNow() != null) {
                score = (farm.getChickensNow().doubleValue() / farm.getPoultryCapacity().doubleValue()) * 100.0;
            } else if (farm.getPoultryCapacity() != null) {
                score = farm.getPoultryCapacity().doubleValue();
            }

            String enterpriseKey = KEY_PREFIX + farm.getIdEnterprise();
            redisTemplate.opsForZSet().add(enterpriseKey, farm.getId().toString(), score);
            redisTemplate.opsForZSet().add(KEY_GLOBAL, farm.getId().toString(), score);
            count++;
        }

        log.info("Sincronizadas {} fazendas para o Redis Ranking", count);
        return count;
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
}

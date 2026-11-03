package com.ourosapp.springapi.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.event.RankingUpdatedEvent;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ouvinte de eventos que reage ao recálculo de ranking de fazendas para disparar
 * notificações In-App e Push Notifications (FCM) aos produtores rurais e funcionários de integradora.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingNotificationListener {

    private final FarmOwnerRepository farmOwnerRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRankingUpdated(RankingUpdatedEvent event) {
        if (event == null || event.rankings() == null || event.rankings().isEmpty()) {
            return;
        }

        Long enterpriseId = event.enterpriseId();
        List<FarmRankingResponseDTO> rankings = event.rankings();

        log.info("Processando notificações de recálculo de ranking da integradora ID {} ({} fazendas)...",
                enterpriseId, rankings != null ? rankings.size() : 0);

        try {
            notifyFarmOwners(rankings);
            notifyCompanyEmployees(enterpriseId, rankings);
        } catch (Exception ex) {
            log.error("Erro durante o processamento de notificações de ranking: {}", ex.getMessage(), ex);
        }
    }

    private void notifyFarmOwners(List<FarmRankingResponseDTO> rankings) {
        if (rankings == null || rankings.isEmpty()) {
            return;
        }

        List<Long> farmIds = rankings.stream()
                .map(FarmRankingResponseDTO::farmId)
                .filter(Objects::nonNull)
                .toList();

        if (farmIds.isEmpty()) {
            return;
        }

        List<FarmOwner> owners = farmOwnerRepository.findAllByIdFarmIn(farmIds);
        if (owners.isEmpty()) {
            log.info("Nenhum produtor rural encontrado para as fazendas ranqueadas.");
            return;
        }

        Map<Long, FarmRankingResponseDTO> rankingByFarm = rankings.stream()
                .filter(r -> r.farmId() != null)
                .collect(Collectors.toMap(FarmRankingResponseDTO::farmId, r -> r, (r1, r2) -> r1));

        for (FarmOwner owner : owners) {
            if (owner.getId() == null || owner.getIdFarm() == null) {
                continue;
            }

            FarmRankingResponseDTO rank = rankingByFarm.get(owner.getIdFarm());
            if (rank == null) {
                continue;
            }

            long position = rank.rankPosition() != null ? rank.rankPosition() : 1L;
            String farmName = rank.farmName() != null ? rank.farmName() : "Sua fazenda";
            String medalBadge = getMedalBadge(position);

            String title = "Ranking Atualizado!";
            String message = String.format(
                    "A fazenda %s alcançou a %dª posição no ranking%s. Confira seu desempenho no aplicativo!",
                    farmName, position, medalBadge
            );

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("type", "RANKING_UPDATED");
            payloadMap.put("farm_id", String.valueOf(rank.farmId()));
            payloadMap.put("farm_name", farmName);
            payloadMap.put("rank_position", String.valueOf(position));
            if (rank.score() != null) {
                payloadMap.put("score", String.valueOf(rank.score()));
            }

            String jsonPayload = toJson(payloadMap);

            notificationService.createNotificationWithPush(
                    owner.getId(),
                    RoleConstants.FARM_OWNER,
                    title,
                    message,
                    "RANKING_UPDATED",
                    null,
                    jsonPayload
            );
        }
    }

    private void notifyCompanyEmployees(Long enterpriseId, List<FarmRankingResponseDTO> rankings) {
        if (enterpriseId == null) {
            return;
        }

        List<CompanyEmployee> employees = companyEmployeeRepository.findAllByIdEnterprise(enterpriseId);
        if (employees.isEmpty()) {
            return;
        }

        int totalFarms = rankings != null ? rankings.size() : 0;
        String title = "Ranking de Fazendas Atualizado";
        String message = String.format(
                "O ranking de fazendas da sua integradora foi recalculado (%d granjas participantes). Confira os novos resultados!",
                totalFarms
        );

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("type", "RANKING_UPDATED");
        payloadMap.put("enterprise_id", String.valueOf(enterpriseId));
        payloadMap.put("total_farms", String.valueOf(totalFarms));

        String jsonPayload = toJson(payloadMap);

        // Cria notificação In-App para cada funcionário
        for (CompanyEmployee emp : employees) {
            if (emp.getId() != null) {
                notificationService.createNotificationWithPush(
                        emp.getId(),
                        RoleConstants.COMPANY_EMPLOYEE,
                        title,
                        message,
                        "RANKING_UPDATED",
                        null,
                        jsonPayload
                );
            }
        }
    }

    private String getMedalBadge(long position) {
        if (position == 1) return " (Medalha de Ouro 🥇)";
        if (position == 2) return " (Medalha de Prata 🥈)";
        if (position == 3) return " (Medalha de Bronze 🥉)";
        return "";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar payload de notificação para JSON: {}", e.getMessage());
            return "{}";
        }
    }
}

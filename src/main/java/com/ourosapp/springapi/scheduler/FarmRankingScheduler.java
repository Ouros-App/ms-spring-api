package com.ourosapp.springapi.scheduler;

import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.service.FarmRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agendador semanal para recálculo e sincronização automática dos rankings de fazendas no Redis NoSQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FarmRankingScheduler {

    private final FarmRankingService farmRankingService;
    private final EnterpriseRepository enterpriseRepository;

    /**
     * Executa a cada 7 dias (configurável via app.ranking.cron).
     * Itera por todas as empresas integradoras cadastradas e recalcula os rankings no Redis.
     */
    @Scheduled(cron = "${app.ranking.cron:0 0 0 */7 * *}", zone = "${app.ranking.cron-zone:America/Sao_Paulo}")
    public void scheduleWeeklyRankingCalculation() {
        log.info("Iniciando rotina agendada de recálculo semanal de rankings de fazendas...");
        try {
            List<Enterprise> enterprises = enterpriseRepository.findAll();
            long totalFarmsSynced = 0;

            for (Enterprise enterprise : enterprises) {
                if (enterprise.getId() != null) {
                    try {
                        var rankings = farmRankingService.recalculateAndCacheEnterpriseRanking(enterprise.getId());
                        totalFarmsSynced += rankings.size();
                        log.info("Ranking da empresa ID {} recalculado com sucesso ({} fazendas)",
                                enterprise.getId(), rankings.size());
                    } catch (Exception ex) {
                        log.error("Erro ao recalcular ranking da empresa ID {}: {}", enterprise.getId(), ex.getMessage(), ex);
                    }
                }
            }
            log.info("Rotina semanal de ranqueamento concluída. Total de fazendas sincronizadas: {}", totalFarmsSynced);
        } catch (Exception ex) {
            log.error("Falha geral na execução do job agendado de ranking: {}", ex.getMessage(), ex);
        }
    }
}

package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.EnterprisePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações na tabela "enterprise_plans".
 */
@Repository
public interface EnterprisePlanRepository extends JpaRepository<EnterprisePlan, Long> {

    boolean existsByIdEnterpriseAndIdPlan(Long idEnterprise, Long idPlan);

    Optional<EnterprisePlan> findByIdEnterpriseAndIdPlan(Long idEnterprise, Long idPlan);

    List<EnterprisePlan> findByIdEnterprise(Long idEnterprise);

    List<EnterprisePlan> findByIdPlan(Long idPlan);

    List<EnterprisePlan> findByIdEnterpriseAndIdPlanIn(Long idEnterprise, List<Long> idPlans);
}

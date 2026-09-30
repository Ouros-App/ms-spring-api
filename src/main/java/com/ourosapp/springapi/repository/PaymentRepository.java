package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações na tabela "payments".
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByIdEnterprise(Long idEnterprise);

    List<Payment> findByIdEnterprisePlan(Long idEnterprisePlan);

    List<Payment> findByIdEnterpriseAndIdEnterprisePlan(Long idEnterprise, Long idEnterprisePlan);

    boolean existsByIdEnterprisePlan(Long idEnterprisePlan);
}

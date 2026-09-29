package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa o vínculo e contratação de um Plano de Assinatura por uma Empresa Integradora.
 * Mapeada para a tabela "enterprise_plans" no banco de dados relacional.
 */
@Entity
@Table(name = "enterprise_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enterprise_plans_enterprise_plan", columnNames = {"id_enterprise", "id_plan"}),
        @UniqueConstraint(name = "uk_enterprise_plans_id_enterprise", columnNames = {"id", "id_enterprise"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterprisePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;

    @Column(name = "id_plan", nullable = false)
    private Long idPlan;
}

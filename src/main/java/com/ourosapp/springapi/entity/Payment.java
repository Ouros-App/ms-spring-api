package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade JPA que representa uma transação de pagamento de um plano por uma Empresa Integradora.
 * Mapeada para a tabela "payments" no banco de dados relacional.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "value", precision = 10, scale = 2, nullable = false)
    private BigDecimal value;

    @Builder.Default
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;

    @Column(name = "id_enterprise_plan", nullable = false)
    private Long idEnterprisePlan;
}

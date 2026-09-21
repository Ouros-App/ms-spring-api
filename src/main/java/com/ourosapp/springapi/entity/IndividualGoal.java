package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade JPA que representa uma Meta Individual de uma Fazenda no sistema Ouros App.
 * Mapeada para a tabela "individual_goals" no banco de dados relacional.
 */
@Entity
@Table(name = "individual_goals")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 50, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "target_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal targetValue;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

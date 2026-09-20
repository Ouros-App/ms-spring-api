package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade JPA que representa uma Meta Estadual no sistema Ouros App.
 * Mapeada para a tabela "state_goals" no banco de dados relacional.
 */
@Entity
@Table(name = "state_goals")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 50, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "status", length = 40, nullable = false)
    private String status;

    @Column(name = "target_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal targetValue;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    @Column(name = "date_end", nullable = false)
    private LocalDate dateEnd;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

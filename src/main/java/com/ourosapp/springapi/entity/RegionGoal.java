package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa a associação de uma Região a uma Meta Estadual no sistema Ouros App.
 * Mapeada para a tabela "regions_goals" no banco de dados relacional.
 */
@Entity
@Table(name = "regions_goals")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region", length = 50, nullable = false)
    private String region;

    @Column(name = "id_goal", nullable = false)
    private Long idGoal;
}

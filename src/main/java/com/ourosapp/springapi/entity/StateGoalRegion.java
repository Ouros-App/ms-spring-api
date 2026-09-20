package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa a tabela de junção entre uma Meta Estadual e uma Região no sistema Ouros App.
 * Mapeada para a tabela "state_goal_regions" no banco de dados relacional.
 */
@Entity
@Table(name = "state_goal_regions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_goal", "id_region"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateGoalRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_goal", nullable = false)
    private Long idGoal;

    @Column(name = "id_region", nullable = false)
    private Long idRegion;
}

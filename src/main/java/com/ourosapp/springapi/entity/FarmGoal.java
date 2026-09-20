package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa o vínculo entre uma Fazenda e uma Meta Estadual no sistema Ouros App.
 * Mapeada para a tabela "farm_goals" no banco de dados relacional.
 */
@Entity
@Table(name = "farm_goals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_farm", "id_goal"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;

    @Column(name = "id_goal", nullable = false)
    private Long idGoal;
}

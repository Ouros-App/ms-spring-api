package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa o vínculo N:M entre uma Dica Técnica e uma Categoria no sistema Ouros App.
 * Mapeada para a tabela "tip_categories" no banco de dados relacional.
 */
@Entity
@Table(name = "tip_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_tip", "id_category"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_tip", nullable = false)
    private Long idTip;

    @Column(name = "id_category", nullable = false)
    private Long idCategory;
}

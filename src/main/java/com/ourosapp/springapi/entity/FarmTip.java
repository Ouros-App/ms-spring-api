package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa o vínculo N:M entre uma Fazenda e uma Dica Técnica no sistema Ouros App.
 * Mapeada para a tabela "farms_tips" no banco de dados relacional.
 */
@Entity
@Table(name = "farms_tips", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_farm", "id_tip"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;

    @Column(name = "id_tip", nullable = false)
    private Long idTip;
}

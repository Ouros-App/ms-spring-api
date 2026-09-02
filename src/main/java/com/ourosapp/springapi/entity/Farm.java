package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade JPA que representa uma Fazenda no sistema Ouros App.
 * Mapeada para a tabela "farms" no banco de dados relacional.
 */
@Entity
@Table(name = "farms")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "area_property", nullable = false, precision = 19, scale = 4)
    private BigDecimal areaProperty;

    @Column(name = "region", nullable = false, length = 50)
    private String region;

    @Column(name = "poultry_capacity", nullable = false)
    private Integer poultryCapacity;

    @Column(name = "place", nullable = false, length = 50)
    private String place;

    @Column(name = "id_address", nullable = false)
    private Long idAddress;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;
}

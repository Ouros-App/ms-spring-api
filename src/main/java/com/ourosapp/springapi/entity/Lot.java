package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade JPA que representa um Lote de Aves no sistema Ouros App.
 * Mapeada para a tabela "lots" no banco de dados relacional.
 */
@Entity
@Table(name = "lots")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "received_chickens", nullable = false)
    private Integer receivedChickens;

    @Column(name = "delivered_chickens", nullable = false)
    private Integer deliveredChickens;

    @Column(name = "date_birth", nullable = false)
    private LocalDate dateBirth;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "gain", nullable = false, precision = 19, scale = 4)
    private BigDecimal gain;

    @Column(name = "losts", nullable = false)
    private Integer losts;

    @Column(name = "cost", nullable = false)
    private Double cost;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade JPA que representa um Registro de Medição de Água no sistema Ouros App.
 * Mapeada para a tabela "water_registries" no banco de dados relacional.
 */
@Entity
@Table(name = "water_registries")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaterRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "start_hydrometer", nullable = false, precision = 19, scale = 4)
    private BigDecimal startHydrometer;

    @Column(name = "end_hydrometer", nullable = false, precision = 19, scale = 4)
    private BigDecimal endHydrometer;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

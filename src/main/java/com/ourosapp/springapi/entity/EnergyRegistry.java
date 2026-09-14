package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade JPA que representa um Registro de Consumo de Energia Elétrica no sistema Ouros App.
 * Mapeada para a tabela "energy_registries" no banco de dados relacional.
 */
@Entity
@Table(name = "energy_registries")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnergyRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Column(name = "energy_consumption", nullable = false)
    private BigDecimal energyConsumption;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

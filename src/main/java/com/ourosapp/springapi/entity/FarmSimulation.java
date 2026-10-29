package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidade JPA mapeada para a tabela {@code farm_simulations}.
 * Representa um cenário de simulação financeira e operacional (água, energia e CGI)
 * salvo para uma granja/fazenda avícola.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "farm_simulations")
public class FarmSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;

    @Column(name = "id_lot", nullable = false, length = 50)
    private String idLot;

    @Column(name = "current_cgi", nullable = false)
    private Double currentCgi;

    @Column(name = "target_cgi", nullable = false)
    private Double targetCgi;

    @Column(name = "water_price", nullable = false)
    private Double waterPrice;

    @Column(name = "energy_price", nullable = false)
    private Double energyPrice;

    @Column(name = "poultry_quantity", nullable = false)
    private Integer poultryQuantity;

    @Column(name = "target_energy_kwh", nullable = false)
    private Double targetEnergyKwh;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

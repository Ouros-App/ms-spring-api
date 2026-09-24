package com.ourosapp.springapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Entidade JPA que representa o registro de Saída e Baixa de Aves no sistema Ouros App.
 * Mapeada para a tabela "chicken_left" no banco de dados relacional.
 */
@Entity
@Table(name = "chicken_left")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChickenLeft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chickens_count", nullable = false)
    private Integer chickensCount;

    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

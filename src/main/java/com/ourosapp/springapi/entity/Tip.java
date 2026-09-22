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

/**
 * Entidade JPA que representa uma Dica Técnica no sistema Ouros App.
 * Mapeada para a tabela "tips" no banco de dados relacional.
 */
@Entity
@Table(name = "tips")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tip", columnDefinition = "TEXT", nullable = false)
    private String tip;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

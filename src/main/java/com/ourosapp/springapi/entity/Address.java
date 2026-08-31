package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa um endereço físico no sistema Ouros App.
 * Mapeada para a tabela "addresses" no banco de dados relacional.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zip_code", length = 50, nullable = false)
    private String zipCode;

    @Column(name = "state", length = 2, nullable = false)
    private String state;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "number", length = 50, nullable = false)
    private String number;

    @Column(name = "country", length = 2, nullable = false)
    private String country;
}

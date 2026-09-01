package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa uma Empresa Integradora no sistema Ouros App.
 * Mapeada para a tabela "enterprises" no banco de dados relacional.
 */
@Entity
@Table(name = "enterprises")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "document_number", length = 14, nullable = false, unique = true)
    private String documentNumber;

    @Column(name = "telephone", length = 13, nullable = false)
    private String telephone;

    @Column(name = "id_address", nullable = false)
    private Long idAddress;
}


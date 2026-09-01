package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa um Produtor Rural / Proprietário de Fazenda no sistema Ouros App.
 * Mapeada para a tabela "farm_owners" no banco de dados relacional.
 */
@Entity
@Table(name = "farm_owners")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @ToString.Exclude
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "document_number", unique = true, nullable = false)
    private String documentNumber;

    @Column(name = "telephone", length = 20, nullable = false)
    private String telephone;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade que representa um Dono de Fazenda.
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

    @Column(name = "name", length = 100)
    private String name;

    @ToString.Exclude
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "document_number", unique = true)
    private String documentNumber;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Builder.Default
    @Column(name = "first_access", nullable = false)
    private Boolean firstAccess = Boolean.TRUE;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farm_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", length = 32, nullable = false, unique = true)
    private String email;

    @Column(name = "document_number", unique = true, nullable = false)
    private String documentNumber;

    @Column(name = "telephone", length = 13, nullable = false)
    private String telephone;

    @Column(name = "id_farm", nullable = false)
    private Long idFarm;
}

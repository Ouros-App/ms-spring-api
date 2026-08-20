package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @Column(name = "document_number", unique = true, nullable = false)
    private String documentNumber;

    @Column(name = "email", length = 32, nullable = false, unique = true)
    private String email;

    @Column(name = "telephone", length = 13, nullable = false)
    private String telephone;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa um Funcionário de Empresa Integradora no sistema Ouros App.
 * Mapeada para a tabela "company_employees" no banco de dados relacional.
 */
@Entity
@Table(name = "company_employees")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "document_number", length = 11, unique = true, nullable = false)
    private String documentNumber;

    @Column(name = "email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "telephone", length = 13, nullable = false)
    private String telephone;

    @ToString.Exclude
    @Column(name = "password")
    private String password;

    @Column(name = "id_enterprise", nullable = false)
    private Long idEnterprise;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    public CompanyEmployee(Long id, String name, String documentNumber, String email, String telephone, String password, Long idEnterprise) {
        this.id = id;
        this.name = name;
        this.documentNumber = documentNumber;
        this.email = email;
        this.telephone = telephone;
        this.password = password;
        this.idEnterprise = idEnterprise;
    }
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;
}

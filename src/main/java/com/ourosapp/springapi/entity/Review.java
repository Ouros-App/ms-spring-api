package com.ourosapp.springapi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade JPA que representa uma Avaliação/Comentário de uma Dica Técnica no sistema Ouros App.
 * Mapeada para a tabela "reviews" no banco de dados relacional.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "id_tip", nullable = false)
    private Long idTip;
}

package com.ourosapp.springapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(name = "title", length = 200, nullable = false)
    @Builder.Default
    private String title = "Dica Operacional";

    @Column(name = "tip", columnDefinition = "TEXT", nullable = false)
    private String tip;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @PrePersist
    @PreUpdate
    public void syncImageUrls() {
        if (this.imageUrl != null && this.fotoUrl == null) {
            this.fotoUrl = this.imageUrl;
        } else if (this.fotoUrl != null && this.imageUrl == null) {
            this.imageUrl = this.fotoUrl;
        }
    }
}

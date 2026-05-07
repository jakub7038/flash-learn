package com.flashlearn.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    /**
     * Unikalny identyfikator URL-friendly, np. "programowanie", "jezyki".
     * Uzywany jako parametr filtrowania w GET /marketplace?category={slug}
     */
    @Column(nullable = false, length = 50, unique = true)
    private String slug;

    /**
     * Nazwa ikony Material Icons do wyswietlenia w Android.
     * Np. "code", "language", "calculate".
     */
    @Column(name = "icon_name", nullable = false, length = 50)
    private String iconName;
}
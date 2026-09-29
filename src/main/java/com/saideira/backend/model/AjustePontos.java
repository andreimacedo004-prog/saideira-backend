package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pontos a mais (ou a menos) que o admin deu para alguem num desafio.
 * Entra no total do ranking e aparece la com o motivo.
 */
@Entity
@Table(name = "ajustes_pontos")
@Getter
@Setter
@NoArgsConstructor
public class AjustePontos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge desafio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private int pontos;

    @Column(nullable = false, length = 200)
    private String motivo;

    // Quem fez o ajuste (fica nulo se a conta do admin for apagada)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    private User criadoPor;

    @Column(nullable = false)
    private LocalDateTime criadoEm;
}

package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Reacao a um check-in. Cada pessoa pode deixar varias reacoes diferentes
 * no mesmo check-in, mas so uma de cada tipo.
 */
@Entity
@Table(name = "reactions")
@Getter
@Setter
@NoArgsConstructor
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_id", nullable = false)
    private CheckIn checkIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    private LocalDateTime criadoEm = LocalDateTime.now();

    /** O front escolhe o emoji de cada um: BRINDE 🍻, FOGO 🔥, RISADA 😂, LENDA 👑 */
    public enum Tipo {
        BRINDE, FOGO, RISADA, LENDA
    }
}

package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Um role registrado: onde, quando, com quem e o que rolou de cerveja.
 *
 * Nao existe coluna de pontos. A pontuacao e sempre recalculada a partir
 * dos check-ins (ver ScoreService) — assim, apagar um check-in ou mudar
 * uma regra ajusta o ranking sozinho.
 */
@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge desafio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRole tipo;

    // Como o usuario digitou (ex: "Bar do Zé") — e o que aparece no feed
    @Column(nullable = false)
    private String local;

    // Versao normalizada ("bar do ze"), usada no bonus de lugar novo
    @Column(nullable = false)
    private String localNormalizado;

    @Column(length = 1024)
    private String fotoUrl;  // opcional (URL publica, ex: Cloudinary)

    @Column(length = 500)
    private String legenda;  // opcional

    // Quando o role aconteceu (pode ser ate 24h atras, para quem esqueceu de registrar)
    @Column(nullable = false)
    private LocalDateTime feitoEm;

    // Quando o registro foi feito no app
    private LocalDateTime criadoEm = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
        name = "check_in_amigos",
        joinColumns = @JoinColumn(name = "check_in_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> amigosMarcados = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
        name = "check_in_cervejas",
        joinColumns = @JoinColumn(name = "check_in_id"),
        inverseJoinColumns = @JoinColumn(name = "beer_id")
    )
    private Set<Beer> cervejas = new LinkedHashSet<>();

    public enum TipoRole {
        BAR, FESTA, CHURRASCO, SHOW, VISITA, OUTRO
    }
}

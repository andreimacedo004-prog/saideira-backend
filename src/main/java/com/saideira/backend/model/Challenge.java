package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Um desafio com comeco e fim dentro de um grupo
 * (ex: "Roles de Fim de Ano", de 01/11 a 31/12).
 *
 * O ranking e sempre de um desafio: quando ele acaba, o proximo comeca do zero.
 */
@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FriendGroup grupo;

    @Column(nullable = false)
    private String nome;

    // Datas inclusivas: um desafio de 01/11 a 31/12 aceita check-in ate 31/12 23:59
    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private User criadoPor;

    private LocalDateTime criadoEm = LocalDateTime.now();

    // Minutos entre dois check-ins da mesma pessoa. Nulo = padrao do app (2h).
    // So o admin muda (ex.: 30 min no desafio de um show).
    private Integer intervaloMinimoMinutos;

    public boolean contem(LocalDate dia) {
        return !dia.isBefore(dataInicio) && !dia.isAfter(dataFim);
    }
}

package com.saideira.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Uma cerveja dentro de um check-in: qual, em que formato e quantas.
 *
 * Formato e quantidade NAO entram na pontuacao (o bonus e por cerveja nova,
 * nao por volume) e nao aparecem no feed. Servem so para a retrospectiva
 * do fim do desafio: quantos litros cada um tomou, e a galera toda.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CervejaDoRole {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beer_id", nullable = false)
    private Beer cerveja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FormatoCerveja formato;

    @Column(nullable = false)
    private int quantidade;

    public CervejaDoRole(Beer cerveja, FormatoCerveja formato, int quantidade) {
        this.cerveja = cerveja;
        this.formato = formato;
        this.quantidade = quantidade;
    }

    public int mililitros() {
        return formato.getMililitros() * quantidade;
    }

    // Uma cerveja aparece uma vez por check-in (chave do banco: check_in_id + beer_id)
    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;
        if (!(outro instanceof CervejaDoRole that)) return false;
        return cerveja != null && that.cerveja != null && Objects.equals(cerveja.getId(), that.cerveja.getId());
    }

    @Override
    public int hashCode() {
        return cerveja == null ? 0 : Objects.hashCode(cerveja.getId());
    }
}

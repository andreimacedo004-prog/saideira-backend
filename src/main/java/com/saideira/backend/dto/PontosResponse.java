package com.saideira.backend.dto;

import com.saideira.backend.service.ScoreService;

/** Quanto um check-in rendeu e por que — o feed mostra "+23 pts". */
public record PontosResponse(
    int total,
    int cervejasNovas,
    int amigosMarcados,
    boolean lugarNovo
) {
    public static PontosResponse de(ScoreService.PontosCheckIn p) {
        return new PontosResponse(p.total(), p.cervejasNovas(), p.amigosMarcados(), p.lugarNovo());
    }
}

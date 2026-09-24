package com.saideira.backend.dto;

import com.saideira.backend.service.ScoreService;

/**
 * Uma linha do ranking. Alem do total, traz de onde vieram os pontos
 * — da material para o front mostrar "rei das cervejas novas" e afins.
 */
public record RankingItemResponse(
    int posicao,
    UsuarioResumo usuario,
    int pontos,
    int checkIns,
    int cervejasNovas,
    int amigosMarcados,
    int lugaresNovos
) {
    public static RankingItemResponse de(ScoreService.PosicaoRanking p) {
        return new RankingItemResponse(
            p.posicao(), UsuarioResumo.de(p.usuario()), p.pontos(),
            p.checkIns(), p.cervejasNovas(), p.amigosMarcados(), p.lugaresNovos()
        );
    }
}

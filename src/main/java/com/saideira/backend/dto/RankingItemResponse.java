package com.saideira.backend.dto;

import com.saideira.backend.service.ScoreService;

import java.util.List;

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
    int lugaresNovos,
    // Soma dos ajustes do admin (ja incluida em `pontos`) e cada um com o motivo
    int ajuste,
    List<Ajuste> ajustes
) {
    public record Ajuste(int pontos, String motivo) {}

    public static RankingItemResponse de(ScoreService.PosicaoRanking p) {
        return new RankingItemResponse(
            p.posicao(), UsuarioResumo.de(p.usuario()), p.pontos(),
            p.checkIns(), p.cervejasNovas(), p.amigosMarcados(), p.lugaresNovos(),
            p.ajuste(), p.ajustes().stream().map(a -> new Ajuste(a.getPontos(), a.getMotivo())).toList()
        );
    }
}

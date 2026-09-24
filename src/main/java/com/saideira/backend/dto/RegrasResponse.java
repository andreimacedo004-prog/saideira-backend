package com.saideira.backend.dto;

/**
 * As regras do jogo, para a tela "como pontuar" do app.
 * Vem da API (e nao escrito no front) para as duas pontas nunca discordarem.
 */
public record RegrasResponse(
    int pontosPorCheckIn,
    int pontosPorCervejaNova,
    int pontosPorAmigoMarcado,
    int pontosPorLugarNovo,
    long intervaloMinimoMinutos,
    long maxHorasRetroativo,
    int maxCervejasPorCheckIn
) {}

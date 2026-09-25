package com.saideira.backend.dto;

import com.saideira.backend.model.FormatoCerveja;

import java.util.List;

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
    int maxCervejasPorCheckIn,
    List<Formato> formatos
) {
    /** Opcoes do seletor de formato (lata, garrafa...) com o volume de cada uma. */
    public record Formato(FormatoCerveja valor, String rotulo, int mililitros) {}
}

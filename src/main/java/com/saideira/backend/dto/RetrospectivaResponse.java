package com.saideira.backend.dto;

import java.util.List;

/**
 * Numeros para a retrospectiva do desafio (o "Saideira Wrapped").
 *
 * O volume de cada pessoa so vai para ela mesma (`eu`); da galera sai
 * apenas o total coletivo (`grupo`). De proposito, nao existe ranking de
 * quem bebeu mais — o ranking do app e de role, variedade e galera.
 */
public record RetrospectivaResponse(
    Long desafioId,
    Consumo eu,
    Consumo grupo,
    List<CervejaConsumida> minhasCervejas
) {
    public record Consumo(int roles, int unidades, double litros, int cervejasDiferentes) {}

    public record CervejaConsumida(CervejaResponse cerveja, int unidades, double litros) {}
}

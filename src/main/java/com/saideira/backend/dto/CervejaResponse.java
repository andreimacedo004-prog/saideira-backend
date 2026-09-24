package com.saideira.backend.dto;

import com.saideira.backend.model.Beer;

public record CervejaResponse(
    Long id,
    String nome,
    String estilo,
    String cervejaria
) {
    public static CervejaResponse de(Beer b) {
        return new CervejaResponse(b.getId(), b.getNome(), b.getEstilo(), b.getCervejaria());
    }
}

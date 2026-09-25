package com.saideira.backend.dto;

import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FormatoCerveja;
import com.saideira.backend.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O check-in como o autor preencheu, para abrir a tela de edicao.
 * Diferente do card do feed, traz formato e quantidade das cervejas —
 * por isso so o autor recebe.
 */
public record CheckInParaEditarResponse(
    Long id,
    Long desafioId,
    CheckIn.TipoRole tipo,
    String local,
    String fotoUrl,
    String legenda,
    LocalDateTime feitoEm,
    List<Long> amigosIds,
    List<Item> cervejas
) {
    public record Item(CervejaResponse cerveja, FormatoCerveja formato, int quantidade) {}

    public static CheckInParaEditarResponse de(CheckIn c) {
        return new CheckInParaEditarResponse(
            c.getId(),
            c.getDesafio().getId(),
            c.getTipo(),
            c.getLocal(),
            c.getFotoUrl(),
            c.getLegenda(),
            c.getFeitoEm(),
            c.getAmigosMarcados().stream().map(User::getId).toList(),
            c.getCervejas().stream()
                .map(item -> new Item(CervejaResponse.de(item.getCerveja()), item.getFormato(), item.getQuantidade()))
                .toList()
        );
    }
}

package com.saideira.backend.dto;

import com.saideira.backend.model.Challenge;

import java.time.LocalDate;

public record DesafioResponse(
    Long id,
    Long grupoId,
    String grupoNome,
    String nome,
    LocalDate dataInicio,
    LocalDate dataFim,
    Status status,
    // Para a tela saber se mostra "Editar desafio" (so quem criou pode)
    Long criadoPorId
) {
    public enum Status {
        EM_BREVE, ATIVO, ENCERRADO
    }

    public static DesafioResponse de(Challenge c, LocalDate hoje) {
        Status status;
        if (hoje.isBefore(c.getDataInicio())) {
            status = Status.EM_BREVE;
        } else if (hoje.isAfter(c.getDataFim())) {
            status = Status.ENCERRADO;
        } else {
            status = Status.ATIVO;
        }
        return new DesafioResponse(
            c.getId(), c.getGrupo().getId(), c.getGrupo().getNome(),
            c.getNome(), c.getDataInicio(), c.getDataFim(), status,
            c.getCriadoPor().getId()
        );
    }
}

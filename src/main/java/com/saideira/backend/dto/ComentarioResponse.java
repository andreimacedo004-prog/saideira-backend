package com.saideira.backend.dto;

import com.saideira.backend.model.Comment;

import java.time.LocalDateTime;

public record ComentarioResponse(
    Long id,
    Long checkInId,
    UsuarioResumo autor,
    String texto,
    LocalDateTime criadoEm
) {
    public static ComentarioResponse de(Comment c) {
        return new ComentarioResponse(
            c.getId(), c.getCheckIn().getId(), UsuarioResumo.de(c.getAutor()), c.getTexto(), c.getCriadoEm()
        );
    }
}

package com.saideira.backend.dto;

import com.saideira.backend.model.User;

/** Como um amigo aparece no feed, no grupo e no ranking — sem e-mail. */
public record UsuarioResumo(
    Long id,
    String nome,
    String fotoUrl
) {
    public static UsuarioResumo de(User u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getFotoUrl());
    }
}

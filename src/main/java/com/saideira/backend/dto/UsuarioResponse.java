package com.saideira.backend.dto;

import com.saideira.backend.model.User;

/**
 * O proprio perfil (tela "eu"). E o unico lugar onde o e-mail aparece:
 * no feed e nos grupos os amigos veem so o UsuarioResumo.
 */
public record UsuarioResponse(
    Long id,
    String email,
    String nome,
    String fotoUrl,
    String bio
) {
    public static UsuarioResponse de(User u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getNome(), u.getFotoUrl(), u.getBio());
    }
}

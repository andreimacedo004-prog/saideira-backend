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
    String bio,
    // So para o app mostrar o menu "Admin"; quem decide o acesso e o servidor
    boolean admin
) {
    public static UsuarioResponse de(User u, boolean admin) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getNome(), u.getFotoUrl(), u.getBio(), admin);
    }
}

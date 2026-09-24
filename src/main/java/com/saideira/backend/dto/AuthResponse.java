package com.saideira.backend.dto;

/**
 * Resposta do cadastro e do login.
 * O app guarda o token e manda em todas as chamadas seguintes:
 * Authorization: Bearer <token>
 */
public record AuthResponse(
    String token,
    long expiraEmMs,
    UsuarioResponse usuario
) {}

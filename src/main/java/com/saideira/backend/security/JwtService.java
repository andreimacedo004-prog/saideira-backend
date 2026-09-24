package com.saideira.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Gera e valida o token JWT.
 * O token carrega o e-mail no 'subject' e o id do usuario num claim extra.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long validadeMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.validade-ms}") long validadeMs
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret precisa ter no minimo 32 caracteres (256 bits) para HS256. "
                + "Gere um com: openssl rand -base64 48"
            );
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.validadeMs = validadeMs;
    }

    public String gerarToken(Long usuarioId, String email) {
        Date agora = new Date();
        return Jwts.builder()
            .subject(email)
            .claim("uid", usuarioId)
            .issuedAt(agora)
            .expiration(new Date(agora.getTime() + validadeMs))
            .signWith(chave)
            .compact();
    }

    /** Retorna os claims do token, ou null se ele for invalido/expirado. */
    public Claims lerClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception ex) {
            return null; // assinatura invalida, token expirado ou malformado
        }
    }

    public long getValidadeMs() {
        return validadeMs;
    }
}

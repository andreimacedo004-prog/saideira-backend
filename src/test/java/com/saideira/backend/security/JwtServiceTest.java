package com.saideira.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    @DisplayName("Token gerado carrega e-mail e id do usuario")
    void geraELeToken() {
        JwtService service = new JwtService(SECRET, 60_000);

        Claims claims = service.lerClaims(service.gerarToken(7L, "ana@exemplo.com"));

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("ana@exemplo.com");
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Token assinado com outro segredo e rejeitado")
    void rejeitaAssinaturaInvalida() {
        JwtService emissor = new JwtService(SECRET, 60_000);
        JwtService outro = new JwtService("outro-segredo-completamente-diferente-32", 60_000);

        assertThat(outro.lerClaims(emissor.gerarToken(7L, "ana@exemplo.com"))).isNull();
    }

    @Test
    @DisplayName("Texto qualquer no lugar do token nao derruba a aplicacao")
    void rejeitaLixo() {
        assertThat(new JwtService(SECRET, 60_000).lerClaims("nao-e-um-token")).isNull();
    }

    @Test
    @DisplayName("Segredo curto demais impede a aplicacao de subir")
    void recusaSegredoFraco() {
        assertThatThrownBy(() -> new JwtService("curto", 60_000))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }
}

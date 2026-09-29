package com.saideira.backend.security;

import com.saideira.backend.exception.MuitasTentativasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteDeTentativasTest {

    /** Relogio que da para adiantar no teste. */
    private static class Relogio extends Clock {
        private Instant agora = Instant.parse("2026-11-07T23:00:00Z");
        void andar(Duration d) { agora = agora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private final Relogio relogio = new Relogio();
    private final LimiteDeTentativas limite = new LimiteDeTentativas(relogio);

    private void errar(String email, String ip, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.verificar(email, ip);
            limite.registrarFalha(email, ip);
        }
    }

    @Test
    @DisplayName("Depois de 5 senhas erradas o e-mail espera 15 min, e a mensagem diz quanto falta")
    void bloqueiaDepoisDeCinco() {
        errar("ana@teste.com", "1.1.1.1", 5);

        assertThatThrownBy(() -> limite.verificar("ANA@teste.com ", "2.2.2.2"))
            .isInstanceOf(MuitasTentativasException.class)
            .hasMessageContaining("15 minutos");

        relogio.andar(Duration.ofMinutes(14));
        assertThatThrownBy(() -> limite.verificar("ana@teste.com", "1.1.1.1"))
            .hasMessageContaining("1 minuto");

        relogio.andar(Duration.ofMinutes(1));
        assertThatCode(() -> limite.verificar("ana@teste.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Quatro erros ainda deixam tentar; acertar a senha zera a conta")
    void sucessoZera() {
        errar("bia@teste.com", "1.1.1.1", 4);
        assertThatCode(() -> limite.verificar("bia@teste.com", "1.1.1.1")).doesNotThrowAnyException();

        limite.registrarSucesso("bia@teste.com");
        errar("bia@teste.com", "1.1.1.1", 4);
        assertThatCode(() -> limite.verificar("bia@teste.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Um IP que chuta senha de varios e-mails para em 20 erros")
    void limitePorIp() {
        for (int i = 0; i < 20; i++) {
            errar("pessoa" + i + "@teste.com", "6.6.6.6", 1);
        }
        assertThatThrownBy(() -> limite.verificar("outra@teste.com", "6.6.6.6"))
            .isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limite.verificar("outra@teste.com", "7.7.7.7")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Senha temporaria do admin libera o e-mail na hora")
    void liberarDesbloqueia() {
        errar("caio@teste.com", "1.1.1.1", 5);
        limite.liberar("caio@teste.com");
        assertThatCode(() -> limite.verificar("caio@teste.com", "9.9.9.9")).doesNotThrowAnyException();
    }
}

package com.saideira.backend.security;

import com.saideira.backend.exception.MuitasTentativasException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freio contra chute de senha no login.
 *
 *  - 5 senhas erradas para o mesmo e-mail em 15 min: esse e-mail espera o fim da janela;
 *  - 20 erros vindos do mesmo IP em 15 min: esse IP espera (quem tenta varios e-mails).
 *
 * Login certo zera o contador do e-mail. Fica em memoria: um deploy novo
 * zera tudo, o que e aceitavel para um app entre amigos com uma instancia so.
 */
@Component
public class LimiteDeTentativas {

    static final int MAX_POR_EMAIL = 5;
    static final int MAX_POR_IP = 20;
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private record Janela(Instant inicio, int falhas) {}

    private final Map<String, Janela> porEmail = new ConcurrentHashMap<>();
    private final Map<String, Janela> porIp = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteDeTentativas(Clock clock) {
        this.clock = clock;
    }

    /** Chamar ANTES de conferir a senha. Lanca 429 se o e-mail ou o IP estiverem bloqueados. */
    public void verificar(String email, String ip) {
        Instant agora = clock.instant();
        verificar(porEmail, chave(email), MAX_POR_EMAIL, agora);
        verificar(porIp, ip, MAX_POR_IP, agora);
    }

    public void registrarFalha(String email, String ip) {
        Instant agora = clock.instant();
        somar(porEmail, chave(email), agora);
        somar(porIp, ip, agora);
        limparSeCheio(agora);
    }

    public void registrarSucesso(String email) {
        porEmail.remove(chave(email));
    }

    /** Usado quando o admin gera uma senha temporaria: a pessoa entra na hora. */
    public void liberar(String email) {
        porEmail.remove(chave(email));
    }

    private void verificar(Map<String, Janela> mapa, String chave, int maximo, Instant agora) {
        if (chave == null) return;
        Janela janela = mapa.get(chave);
        if (janela == null || expirou(janela, agora) || janela.falhas() < maximo) return;
        long segundos = Duration.between(agora, janela.inicio().plus(JANELA)).toSeconds();
        throw new MuitasTentativasException(Math.max(1, (segundos + 59) / 60));
    }

    private void somar(Map<String, Janela> mapa, String chave, Instant agora) {
        if (chave == null) return;
        mapa.compute(chave, (k, atual) -> atual == null || expirou(atual, agora)
            ? new Janela(agora, 1)
            : new Janela(atual.inicio(), atual.falhas() + 1));
    }

    private boolean expirou(Janela janela, Instant agora) {
        return !agora.isBefore(janela.inicio().plus(JANELA));
    }

    private void limparSeCheio(Instant agora) {
        if (porEmail.size() > LIMPAR_ACIMA_DE) porEmail.values().removeIf(j -> expirou(j, agora));
        if (porIp.size() > LIMPAR_ACIMA_DE) porIp.values().removeIf(j -> expirou(j, agora));
    }

    private static String chave(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

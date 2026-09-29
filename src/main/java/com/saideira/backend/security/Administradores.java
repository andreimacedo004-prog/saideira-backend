package com.saideira.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quem e admin: as contas cujo e-mail esta em APP_ADMIN_EMAILS
 * (variavel do Railway, separada por virgula).
 *
 * Nao existe senha de admin guardada em lugar nenhum: o admin entra com a
 * propria conta e o servidor confere o e-mail a cada requisicao. Tirou o
 * e-mail da variavel e reiniciou, o acesso acaba na hora.
 *
 * Cuidado: coloque ali um e-mail que JA tem conta. Um e-mail sem conta
 * poderia ser cadastrado por outra pessoa.
 */
@Component
public class Administradores {

    private final Set<String> emails;

    public Administradores(@Value("${app.admin.emails:}") List<String> emails) {
        this.emails = emails.stream()
            .map(e -> e.trim().toLowerCase())
            .filter(e -> !e.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public boolean eAdmin(String email) {
        return email != null && emails.contains(email.trim().toLowerCase());
    }
}

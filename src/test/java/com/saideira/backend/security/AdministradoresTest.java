package com.saideira.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdministradoresTest {

    @Test
    @DisplayName("Admin e so quem esta na lista, sem ligar para caixa e espacos")
    void reconheceAdmin() {
        Administradores admins = new Administradores(List.of(" Andre@Teste.com ", "", "outro@teste.com"));

        assertThat(admins.eAdmin("andre@teste.com")).isTrue();
        assertThat(admins.eAdmin(" ANDRE@teste.com")).isTrue();
        assertThat(admins.eAdmin("bia@teste.com")).isFalse();
        assertThat(admins.eAdmin(null)).isFalse();
        assertThat(admins.eAdmin("")).isFalse();
    }

    @Test
    @DisplayName("Sem a variavel configurada, ninguem e admin")
    void semLista() {
        Administradores admins = new Administradores(List.of());
        assertThat(admins.eAdmin("andre@teste.com")).isFalse();
    }
}

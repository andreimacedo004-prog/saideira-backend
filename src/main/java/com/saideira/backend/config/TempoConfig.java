package com.saideira.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relogio unico da aplicacao, no fuso de app.fuso-horario.
 *
 * Todo "agora" do codigo sai daqui (LocalDateTime.now(clock)), nunca de
 * LocalDateTime.now() solto. Dois motivos:
 *  1. o servidor no Railway roda em UTC, e o role acontece no horario de Brasilia;
 *  2. nos testes da para congelar o relogio e testar regras de horario sem esperar.
 */
@Configuration
public class TempoConfig {

    @Bean
    public Clock clock(@Value("${app.fuso-horario}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}

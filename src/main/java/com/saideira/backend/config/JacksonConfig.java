package com.saideira.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule aparaTextos() {
        SimpleModule modulo = new SimpleModule("apara-textos");
        modulo.addDeserializer(String.class, new StringDeserializer() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String valor = super.deserialize(p, ctxt);
                return valor == null ? null : valor.trim();
            }
        });
        return modulo;
    }
}

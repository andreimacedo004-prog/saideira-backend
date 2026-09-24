package com.saideira.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SaideiraApplication {

    public static void main(String[] args) {
        String fuso = System.getenv().getOrDefault("APP_FUSO_HORARIO", "America/Sao_Paulo");
        TimeZone.setDefault(TimeZone.getTimeZone(fuso));

        SpringApplication.run(SaideiraApplication.class, args);
    }
}

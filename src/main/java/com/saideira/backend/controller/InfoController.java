package com.saideira.backend.controller;

import com.saideira.backend.dto.RegistrarCheckInRequest;
import com.saideira.backend.dto.RegrasResponse;
import com.saideira.backend.service.ScoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoints publicos: health check e as regras de pontuacao. */
@RestController
@RequestMapping("/api")
public class InfoController {

    private final RegrasResponse regras;

    public InfoController(
        @Value("${app.checkin.intervalo-minimo-minutos}") long intervaloMinimoMinutos,
        @Value("${app.checkin.max-horas-retroativo}") long maxHorasRetroativo
    ) {
        this.regras = new RegrasResponse(
            ScoreService.PONTOS_POR_CHECKIN,
            ScoreService.PONTOS_POR_CERVEJA_NOVA,
            ScoreService.PONTOS_POR_AMIGO_MARCADO,
            ScoreService.PONTOS_POR_LUGAR_NOVO,
            intervaloMinimoMinutos,
            maxHorasRetroativo,
            RegistrarCheckInRequest.MAX_CERVEJAS
        );
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/regras")
    public ResponseEntity<RegrasResponse> regras() {
        return ResponseEntity.ok(regras);
    }
}

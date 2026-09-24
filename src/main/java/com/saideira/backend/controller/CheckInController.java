package com.saideira.backend.controller;

import com.saideira.backend.dto.CheckInResponse;
import com.saideira.backend.dto.RegistrarCheckInRequest;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/desafios/{desafioId}/checkins")
    public ResponseEntity<CheckInResponse> registrar(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long desafioId,
        @Valid @RequestBody RegistrarCheckInRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(checkInService.registrar(logado.getId(), desafioId, request));
    }

    /** Feed do desafio, do role mais recente para o mais antigo, ja com os pontos de cada um. */
    @GetMapping("/desafios/{desafioId}/checkins")
    public ResponseEntity<List<CheckInResponse>> feed(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long desafioId
    ) {
        return ResponseEntity.ok(checkInService.feed(desafioId, logado.getId()));
    }

    @GetMapping("/checkins/{checkInId}")
    public ResponseEntity<CheckInResponse> detalhe(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long checkInId
    ) {
        return ResponseEntity.ok(checkInService.detalhe(checkInId, logado.getId()));
    }

    @DeleteMapping("/checkins/{checkInId}")
    public ResponseEntity<Void> remover(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long checkInId
    ) {
        checkInService.remover(checkInId, logado.getId());
        return ResponseEntity.noContent().build();
    }
}

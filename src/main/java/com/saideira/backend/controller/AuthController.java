package com.saideira.backend.controller;

import com.saideira.backend.dto.AuthResponse;
import com.saideira.backend.dto.CadastroRequest;
import com.saideira.backend.dto.LoginRequest;
import com.saideira.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Endpoints publicos de conta. Os dois devolvem o token do app. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        AuthResponse resposta = authService.cadastrar(request.email(), request.senha(), request.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.senha()));
    }
}

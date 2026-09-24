package com.saideira.backend.controller;

import com.saideira.backend.dto.AtualizarPerfilRequest;
import com.saideira.backend.dto.UsuarioResponse;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Conta do proprio usuario. Quem esta logado vem do token, nunca da URL. */
@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/eu")
    public ResponseEntity<UsuarioResponse> meuPerfil(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(userService.meuPerfil(logado.getId()));
    }

    @PutMapping("/eu/perfil")
    public ResponseEntity<UsuarioResponse> atualizarPerfil(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @Valid @RequestBody AtualizarPerfilRequest request
    ) {
        return ResponseEntity.ok(
            userService.atualizarPerfil(logado.getId(), request.nome(), request.bio(), request.fotoUrl())
        );
    }
}

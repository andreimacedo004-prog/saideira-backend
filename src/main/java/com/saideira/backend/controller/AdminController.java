package com.saideira.backend.controller;

import com.saideira.backend.dto.AdminDtos;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Area de admin. O SecurityConfig so deixa chegar aqui quem tem ROLE_ADMIN
 * (e-mail em APP_ADMIN_EMAILS); todo o resto recebe 403.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<AdminDtos.Resumo> resumo() {
        return ResponseEntity.ok(adminService.resumo());
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminDtos.Usuario>> usuarios(@RequestParam(required = false) String busca) {
        return ResponseEntity.ok(adminService.usuarios(busca));
    }

    @GetMapping("/usuarios/{usuarioId}/exclusao")
    public ResponseEntity<AdminDtos.PreviaExclusao> previaExclusao(
        @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId
    ) {
        return ResponseEntity.ok(adminService.previaExclusao(usuarioId, admin.getId()));
    }

    @DeleteMapping("/usuarios/{usuarioId}")
    public ResponseEntity<Void> excluirUsuario(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId) {
        adminService.excluirUsuario(usuarioId, admin.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuarios/{usuarioId}/senha-temporaria")
    public ResponseEntity<AdminDtos.SenhaTemporaria> senhaTemporaria(
        @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId
    ) {
        return ResponseEntity.ok(adminService.gerarSenhaTemporaria(usuarioId, admin.getId()));
    }

    @GetMapping("/desafios")
    public ResponseEntity<List<AdminDtos.Desafio>> desafios() {
        return ResponseEntity.ok(adminService.desafios());
    }

    @GetMapping("/desafios/{desafioId}")
    public ResponseEntity<AdminDtos.DesafioDetalhe> desafio(@PathVariable Long desafioId) {
        return ResponseEntity.ok(adminService.desafio(desafioId));
    }

    @PostMapping("/desafios/{desafioId}/ajustes")
    public ResponseEntity<AdminDtos.Ajuste> ajustarPontos(
        @AuthenticationPrincipal UsuarioAutenticado admin,
        @PathVariable Long desafioId,
        @Valid @RequestBody AdminDtos.NovoAjuste pedido
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.ajustarPontos(desafioId, admin.getId(), pedido));
    }

    @DeleteMapping("/ajustes/{ajusteId}")
    public ResponseEntity<Void> removerAjuste(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long ajusteId) {
        adminService.removerAjuste(ajusteId, admin.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/checkins/{checkInId}")
    public ResponseEntity<Void> apagarCheckIn(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long checkInId) {
        adminService.apagarCheckIn(checkInId, admin.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comentarios/{comentarioId}")
    public ResponseEntity<Void> apagarComentario(
        @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long comentarioId
    ) {
        adminService.apagarComentario(comentarioId, admin.getId());
        return ResponseEntity.noContent().build();
    }
}

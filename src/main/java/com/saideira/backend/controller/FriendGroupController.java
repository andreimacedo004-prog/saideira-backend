package com.saideira.backend.controller;

import com.saideira.backend.dto.CriarGrupoRequest;
import com.saideira.backend.dto.EntrarNoGrupoRequest;
import com.saideira.backend.dto.FriendGroupResponse;
import com.saideira.backend.dto.RenomearGrupoRequest;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.FriendGroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grupos")
public class FriendGroupController {

    private final FriendGroupService friendGroupService;
    private final String baseUrlConvite;

    public FriendGroupController(
        FriendGroupService friendGroupService,
        @Value("${app.convite.base-url}") String baseUrlConvite
    ) {
        this.friendGroupService = friendGroupService;
        this.baseUrlConvite = baseUrlConvite;
    }

    @PostMapping
    public ResponseEntity<FriendGroupResponse> criar(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @Valid @RequestBody CriarGrupoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(friendGroupService.criar(logado.getId(), request.nome()));
    }

    @GetMapping
    public ResponseEntity<List<FriendGroupResponse>> meusGrupos(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(friendGroupService.gruposDoUsuario(logado.getId()));
    }

    @GetMapping("/{grupoId}")
    public ResponseEntity<FriendGroupResponse> detalhe(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long grupoId
    ) {
        return ResponseEntity.ok(friendGroupService.detalhe(grupoId, logado.getId()));
    }

    /** So quem criou o grupo (ou o admin) muda o nome. */
    @PatchMapping("/{grupoId}")
    public ResponseEntity<FriendGroupResponse> renomear(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long grupoId,
        @Valid @RequestBody RenomearGrupoRequest request
    ) {
        return ResponseEntity.ok(friendGroupService.renomear(grupoId, logado.getId(), logado.isAdmin(), request.nome()));
    }

    /** Link pronto para o botao "chamar a galera" (Web Share API no PWA). So membros geram. */
    @GetMapping("/{grupoId}/convite")
    public ResponseEntity<Map<String, String>> linkDeConvite(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long grupoId
    ) {
        String codigo = friendGroupService.codigoConvite(grupoId, logado.getId());
        return ResponseEntity.ok(Map.of("codigo", codigo, "link", baseUrlConvite + codigo));
    }

    /** Chamado quando alguem abre o link de convite e aceita entrar. */
    @PostMapping("/entrar")
    public ResponseEntity<FriendGroupResponse> entrarPorConvite(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @Valid @RequestBody EntrarNoGrupoRequest request
    ) {
        return ResponseEntity.ok(friendGroupService.entrarPorCodigo(request.codigoConvite(), logado.getId()));
    }
}

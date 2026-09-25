package com.saideira.backend.controller;

import com.saideira.backend.dto.CriarDesafioRequest;
import com.saideira.backend.dto.DesafioResponse;
import com.saideira.backend.dto.RankingItemResponse;
import com.saideira.backend.dto.RenomearDesafioRequest;
import com.saideira.backend.dto.RetrospectivaResponse;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.ChallengeService;
import com.saideira.backend.service.RetrospectivaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final RetrospectivaService retrospectivaService;

    public ChallengeController(ChallengeService challengeService, RetrospectivaService retrospectivaService) {
        this.challengeService = challengeService;
        this.retrospectivaService = retrospectivaService;
    }

    @PostMapping("/grupos/{grupoId}/desafios")
    public ResponseEntity<DesafioResponse> criar(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long grupoId,
        @Valid @RequestBody CriarDesafioRequest request
    ) {
        DesafioResponse desafio = challengeService.criar(
            grupoId, logado.getId(), request.nome(), request.dataInicio(), request.dataFim()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(desafio);
    }

    @GetMapping("/grupos/{grupoId}/desafios")
    public ResponseEntity<List<DesafioResponse>> doGrupo(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long grupoId
    ) {
        return ResponseEntity.ok(challengeService.listarDoGrupo(grupoId, logado.getId()));
    }

    /** Tela inicial: desafios de todos os meus grupos, ativos primeiro. */
    @GetMapping("/desafios")
    public ResponseEntity<List<DesafioResponse>> meus(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(challengeService.meusDesafios(logado.getId()));
    }

    @GetMapping("/desafios/{desafioId}")
    public ResponseEntity<DesafioResponse> detalhe(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long desafioId
    ) {
        return ResponseEntity.ok(challengeService.detalhe(desafioId, logado.getId()));
    }

    /** So quem criou o desafio. So o nome muda. */
    @PatchMapping("/desafios/{desafioId}")
    public ResponseEntity<DesafioResponse> renomear(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long desafioId,
        @Valid @RequestBody RenomearDesafioRequest request
    ) {
        return ResponseEntity.ok(challengeService.renomear(desafioId, logado.getId(), request.nome()));
    }

    /** So quem criou o desafio. Leva junto os check-ins, reacoes e comentarios dele. */
    @DeleteMapping("/desafios/{desafioId}")
    public ResponseEntity<Void> apagar(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long desafioId
    ) {
        challengeService.apagar(desafioId, logado.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Numeros da retrospectiva: meu consumo e o total da galera.
     * Nao existe versao "por pessoa" para os outros verem — de proposito.
     */
    @GetMapping("/desafios/{desafioId}/retrospectiva")
    public ResponseEntity<RetrospectivaResponse> retrospectiva(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long desafioId
    ) {
        return ResponseEntity.ok(retrospectivaService.doDesafio(desafioId, logado.getId()));
    }

    @GetMapping("/desafios/{desafioId}/ranking")
    public ResponseEntity<List<RankingItemResponse>> ranking(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long desafioId
    ) {
        return ResponseEntity.ok(challengeService.ranking(desafioId, logado.getId()));
    }
}

package com.saideira.backend.controller;

import com.saideira.backend.dto.ReacaoResumo;
import com.saideira.backend.model.Reaction;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.ReactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PUT reage, DELETE desfaz. Os dois sao idempotentes e devolvem o resumo
 * atualizado — o front pode mandar de novo sem medo se a rede falhar.
 */
@RestController
@RequestMapping("/api/checkins/{checkInId}/reacoes/{tipo}")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PutMapping
    public ResponseEntity<List<ReacaoResumo>> reagir(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long checkInId,
        @PathVariable Reaction.Tipo tipo
    ) {
        return ResponseEntity.ok(reactionService.reagir(checkInId, logado.getId(), tipo));
    }

    @DeleteMapping
    public ResponseEntity<List<ReacaoResumo>> desfazer(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long checkInId,
        @PathVariable Reaction.Tipo tipo
    ) {
        return ResponseEntity.ok(reactionService.desfazer(checkInId, logado.getId(), tipo));
    }
}

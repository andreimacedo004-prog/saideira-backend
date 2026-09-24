package com.saideira.backend.controller;

import com.saideira.backend.dto.ComentarRequest;
import com.saideira.backend.dto.ComentarioResponse;
import com.saideira.backend.security.UsuarioAutenticado;
import com.saideira.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/checkins/{checkInId}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(
        @AuthenticationPrincipal UsuarioAutenticado logado,
        @PathVariable Long checkInId,
        @Valid @RequestBody ComentarRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commentService.comentar(checkInId, logado.getId(), request.texto()));
    }

    @GetMapping("/checkins/{checkInId}/comentarios")
    public ResponseEntity<List<ComentarioResponse>> listar(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long checkInId
    ) {
        return ResponseEntity.ok(commentService.listar(checkInId, logado.getId()));
    }

    @DeleteMapping("/comentarios/{comentarioId}")
    public ResponseEntity<Void> remover(
        @AuthenticationPrincipal UsuarioAutenticado logado, @PathVariable Long comentarioId
    ) {
        commentService.remover(comentarioId, logado.getId());
        return ResponseEntity.noContent().build();
    }
}

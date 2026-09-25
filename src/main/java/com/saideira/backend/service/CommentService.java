package com.saideira.backend.service;

import com.saideira.backend.dto.ComentarioResponse;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.Comment;
import com.saideira.backend.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Comentarios num check-in. */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CheckInService checkInService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, CheckInService checkInService, UserService userService) {
        this.commentRepository = commentRepository;
        this.checkInService = checkInService;
        this.userService = userService;
    }

    @Transactional
    public ComentarioResponse comentar(Long checkInId, Long autorId, String texto) {
        CheckIn checkIn = checkInService.buscarVisivel(checkInId, autorId);

        Comment comentario = new Comment();
        comentario.setCheckIn(checkIn);
        comentario.setAutor(userService.buscarPorId(autorId));
        comentario.setTexto(texto.trim());

        return ComentarioResponse.de(commentRepository.save(comentario));
    }

    /** Do mais antigo para o mais novo, como numa conversa. */
    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(Long checkInId, Long usuarioId) {
        checkInService.buscarVisivel(checkInId, usuarioId);
        return commentRepository.findDoCheckIn(checkInId).stream()
            .map(ComentarioResponse::de)
            .toList();
    }

    /** Apaga quem escreveu o comentario ou o dono do check-in (moderando o proprio post). */
    @Transactional
    public void remover(Long comentarioId, Long usuarioId) {
        Comment comentario = commentRepository.findById(comentarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado"));

        boolean autorDoComentario = comentario.getAutor().getId().equals(usuarioId);
        boolean donoDoCheckIn = comentario.getCheckIn().getAutor().getId().equals(usuarioId);

        if (!autorDoComentario && !donoDoCheckIn) {
            throw new AcessoNegadoException("Só quem comentou ou o dono do check-in pode apagar");
        }
        commentRepository.delete(comentario);
    }
}

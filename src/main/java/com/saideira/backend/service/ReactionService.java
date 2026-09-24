package com.saideira.backend.service;

import com.saideira.backend.dto.ReacaoResumo;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.Reaction;
import com.saideira.backend.repository.ReactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reacoes no feed. Reagir e desfazer sao idempotentes: reagir duas vezes
 * com o mesmo emoji nao duplica, e desfazer o que nao existe nao da erro.
 * Os dois devolvem o resumo atualizado para o front redesenhar os botoes.
 */
@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final CheckInService checkInService;
    private final UserService userService;

    public ReactionService(
        ReactionRepository reactionRepository,
        CheckInService checkInService,
        UserService userService
    ) {
        this.reactionRepository = reactionRepository;
        this.checkInService = checkInService;
        this.userService = userService;
    }

    @Transactional
    public List<ReacaoResumo> reagir(Long checkInId, Long usuarioId, Reaction.Tipo tipo) {
        CheckIn checkIn = checkInService.buscarVisivel(checkInId, usuarioId);

        if (reactionRepository.findByCheckInIdAndUsuarioIdAndTipo(checkInId, usuarioId, tipo).isEmpty()) {
            Reaction reacao = new Reaction();
            reacao.setCheckIn(checkIn);
            reacao.setUsuario(userService.buscarPorId(usuarioId));
            reacao.setTipo(tipo);
            reactionRepository.saveAndFlush(reacao);
        }
        return resumo(checkInId, usuarioId);
    }

    @Transactional
    public List<ReacaoResumo> desfazer(Long checkInId, Long usuarioId, Reaction.Tipo tipo) {
        checkInService.buscarVisivel(checkInId, usuarioId);

        reactionRepository.findByCheckInIdAndUsuarioIdAndTipo(checkInId, usuarioId, tipo)
            .ifPresent(r -> {
                reactionRepository.delete(r);
                reactionRepository.flush();
            });
        return resumo(checkInId, usuarioId);
    }

    private List<ReacaoResumo> resumo(Long checkInId, Long usuarioId) {
        return ReacaoResumo.agrupar(reactionRepository.resumoPorCheckIn(List.of(checkInId), usuarioId))
            .getOrDefault(checkInId, List.of());
    }
}

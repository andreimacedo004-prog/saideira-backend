package com.saideira.backend.service;

import com.saideira.backend.dto.DesafioResponse;
import com.saideira.backend.dto.RankingItemResponse;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.repository.ChallengeRepository;
import com.saideira.backend.repository.CheckInRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Desafios: criar, listar e montar o ranking. */
@Service
public class ChallengeService {

    private static final long DURACAO_MAXIMA_DIAS = 366;

    private final ChallengeRepository challengeRepository;
    private final CheckInRepository checkInRepository;
    private final FriendGroupService friendGroupService;
    private final UserService userService;
    private final ScoreService scoreService;
    private final Clock clock;

    public ChallengeService(
        ChallengeRepository challengeRepository,
        CheckInRepository checkInRepository,
        FriendGroupService friendGroupService,
        UserService userService,
        ScoreService scoreService,
        Clock clock
    ) {
        this.challengeRepository = challengeRepository;
        this.checkInRepository = checkInRepository;
        this.friendGroupService = friendGroupService;
        this.userService = userService;
        this.scoreService = scoreService;
        this.clock = clock;
    }

    @Transactional
    public DesafioResponse criar(Long grupoId, Long criadorId, String nome, LocalDate inicio, LocalDate fim) {
        FriendGroup grupo = friendGroupService.buscarGrupoDoMembro(grupoId, criadorId);
        LocalDate hoje = LocalDate.now(clock);

        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data de fim precisa ser igual ou depois da data de inicio");
        }
        if (fim.isBefore(hoje)) {
            throw new IllegalArgumentException("Esse desafio ja teria acabado — escolha uma data de fim a partir de hoje");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= DURACAO_MAXIMA_DIAS) {
            throw new IllegalArgumentException("Um desafio pode durar no maximo 1 ano");
        }

        Challenge desafio = new Challenge();
        desafio.setGrupo(grupo);
        desafio.setNome(nome.trim());
        desafio.setDataInicio(inicio);
        desafio.setDataFim(fim);
        desafio.setCriadoPor(userService.buscarPorId(criadorId));

        return DesafioResponse.de(challengeRepository.save(desafio), hoje);
    }

    @Transactional(readOnly = true)
    public List<DesafioResponse> listarDoGrupo(Long grupoId, Long usuarioId) {
        friendGroupService.buscarGrupoDoMembro(grupoId, usuarioId);
        LocalDate hoje = LocalDate.now(clock);
        return challengeRepository.findByGrupoIdOrderByDataInicioDesc(grupoId).stream()
            .map(d -> DesafioResponse.de(d, hoje))
            .toList();
    }

    /** Tela inicial: desafios de todos os grupos do usuario, os ativos primeiro. */
    @Transactional(readOnly = true)
    public List<DesafioResponse> meusDesafios(Long usuarioId) {
        LocalDate hoje = LocalDate.now(clock);
        return challengeRepository.findDoUsuario(usuarioId).stream()
            .map(d -> DesafioResponse.de(d, hoje))
            .sorted((a, b) -> Integer.compare(prioridade(a.status()), prioridade(b.status())))
            .toList();
    }

    @Transactional(readOnly = true)
    public DesafioResponse detalhe(Long desafioId, Long usuarioId) {
        return DesafioResponse.de(buscarDoMembro(desafioId, usuarioId), LocalDate.now(clock));
    }

    /** Ranking do desafio. Todo membro do grupo aparece, mesmo com zero pontos. */
    @Transactional(readOnly = true)
    public List<RankingItemResponse> ranking(Long desafioId, Long usuarioId) {
        Challenge desafio = buscarDoMembro(desafioId, usuarioId);
        List<CheckIn> checkIns = checkInRepository.findDoDesafio(desafio.getId());

        return scoreService.ranking(desafio.getGrupo().getMembros(), checkIns).stream()
            .map(RankingItemResponse::de)
            .toList();
    }

    /**
     * Busca um desafio exigindo que o usuario seja do grupo dele (senao 403).
     * Uso interno dos services: devolve a entidade.
     */
    @Transactional(readOnly = true)
    public Challenge buscarDoMembro(Long desafioId, Long usuarioId) {
        Challenge desafio = challengeRepository.findById(desafioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Desafio nao encontrado"));

        if (!desafio.getGrupo().temMembro(usuarioId)) {
            throw new AcessoNegadoException("Voce nao faz parte do grupo deste desafio");
        }
        return desafio;
    }

    private static int prioridade(DesafioResponse.Status status) {
        return switch (status) {
            case ATIVO -> 0;
            case EM_BREVE -> 1;
            case ENCERRADO -> 2;
        };
    }
}

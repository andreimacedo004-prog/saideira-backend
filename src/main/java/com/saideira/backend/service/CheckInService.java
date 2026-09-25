package com.saideira.backend.service;

import com.saideira.backend.dto.CheckInParaEditarResponse;
import com.saideira.backend.dto.CheckInResponse;
import com.saideira.backend.dto.EditarCheckInRequest;
import com.saideira.backend.dto.ItemCervejaRequest;
import com.saideira.backend.dto.ReacaoResumo;
import com.saideira.backend.dto.RegistrarCheckInRequest;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.Beer;
import com.saideira.backend.model.CervejaDoRole;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.BeerRepository;
import com.saideira.backend.repository.CheckInRepository;
import com.saideira.backend.repository.CommentRepository;
import com.saideira.backend.repository.ReactionRepository;
import com.saideira.backend.util.Normalizador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check-ins: registrar, editar, apagar e montar o feed.
 *
 * Regras de registro (todas testadas em CheckInServiceTest):
 *  - so membro do grupo faz check-in no desafio;
 *  - o role precisa cair dentro do periodo do desafio;
 *  - nada de check-in no futuro, e no maximo 24h retroativo;
 *  - intervalo minimo entre dois check-ins da mesma pessoa (anti-farm);
 *  - so da para marcar amigos do grupo, e nao da para se marcar.
 */
@Service
public class CheckInService {

    // Tolerancia para relogio de celular um pouco adiantado
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CheckInRepository checkInRepository;
    private final ChallengeService challengeService;
    private final BeerRepository beerRepository;
    private final UserService userService;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final ScoreService scoreService;
    private final Clock clock;
    private final Duration intervaloMinimo;
    private final Duration maxRetroativo;

    public CheckInService(
        CheckInRepository checkInRepository,
        ChallengeService challengeService,
        BeerRepository beerRepository,
        UserService userService,
        ReactionRepository reactionRepository,
        CommentRepository commentRepository,
        ScoreService scoreService,
        Clock clock,
        @Value("${app.checkin.intervalo-minimo-minutos}") long intervaloMinimoMinutos,
        @Value("${app.checkin.max-horas-retroativo}") long maxHorasRetroativo
    ) {
        this.checkInRepository = checkInRepository;
        this.challengeService = challengeService;
        this.beerRepository = beerRepository;
        this.userService = userService;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.scoreService = scoreService;
        this.clock = clock;
        this.intervaloMinimo = Duration.ofMinutes(intervaloMinimoMinutos);
        this.maxRetroativo = Duration.ofHours(maxHorasRetroativo);
    }

    @Transactional
    public CheckInResponse registrar(Long autorId, Long desafioId, RegistrarCheckInRequest req) {
        Challenge desafio = challengeService.buscarDoMembro(desafioId, autorId);
        LocalDateTime agora = LocalDateTime.now(clock);
        // Sem fracao de segundo: o banco arredonda nanossegundos, e a resposta
        // do registro ficaria diferente do que volta depois no feed
        LocalDateTime feitoEm = (req.feitoEm() != null ? req.feitoEm() : agora).truncatedTo(ChronoUnit.SECONDS);

        validarHorario(desafio, feitoEm, agora);
        validarIntervalo(autorId, desafio.getId(), feitoEm);

        CheckIn checkIn = new CheckIn();
        checkIn.setAutor(userService.buscarPorId(autorId));
        checkIn.setDesafio(desafio);
        checkIn.setTipo(req.tipo());
        checkIn.setLocal(req.local().trim());
        checkIn.setLocalNormalizado(Normalizador.normalizar(req.local()));
        checkIn.setFotoUrl(vazioViraNulo(req.fotoUrl()));
        checkIn.setLegenda(vazioViraNulo(req.legenda()));
        checkIn.setFeitoEm(feitoEm);
        checkIn.setAmigosMarcados(resolverAmigos(req.amigosIds(), autorId, desafio.getGrupo()));
        checkIn.setCervejas(resolverCervejas(req.cervejas()));

        checkIn = checkInRepository.save(checkIn);

        List<CheckIn> doDesafio = checkInRepository.findDoDesafio(desafio.getId());
        return montar(doDesafio, List.of(checkIn), autorId).get(0);
    }

    /** Feed do desafio, do role mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<CheckInResponse> feed(Long desafioId, Long usuarioId) {
        Challenge desafio = challengeService.buscarDoMembro(desafioId, usuarioId);
        List<CheckIn> doDesafio = checkInRepository.findDoDesafio(desafio.getId());
        return montar(doDesafio, doDesafio, usuarioId);
    }

    @Transactional(readOnly = true)
    public CheckInResponse detalhe(Long checkInId, Long usuarioId) {
        CheckIn checkIn = buscarVisivel(checkInId, usuarioId);
        List<CheckIn> doDesafio = checkInRepository.findDoDesafio(checkIn.getDesafio().getId());
        return montar(doDesafio, List.of(checkIn), usuarioId).get(0);
    }

    /** O check-in como o autor preencheu (com formato e quantidade), para a tela de edicao. */
    @Transactional(readOnly = true)
    public CheckInParaEditarResponse paraEditar(Long checkInId, Long usuarioId) {
        return CheckInParaEditarResponse.de(buscarParaEditar(checkInId, usuarioId));
    }

    /**
     * So o autor edita, e so enquanto o desafio nao acabou (o ranking final
     * nao muda depois). O horario fica como estava. Tudo o mais passa pelas
     * mesmas regras do registro, e os pontos saem recalculados.
     */
    @Transactional
    public CheckInResponse editar(Long checkInId, Long usuarioId, EditarCheckInRequest req) {
        CheckIn checkIn = buscarParaEditar(checkInId, usuarioId);
        Challenge desafio = checkIn.getDesafio();

        // Resolve antes de mexer: se algo for invalido, nada muda
        Set<User> amigos = resolverAmigos(req.amigosIds(), usuarioId, desafio.getGrupo());
        List<CervejaDoRole> cervejas = resolverCervejas(req.cervejas());

        checkIn.setTipo(req.tipo());
        checkIn.setLocal(req.local().trim());
        checkIn.setLocalNormalizado(Normalizador.normalizar(req.local()));
        checkIn.setFotoUrl(vazioViraNulo(req.fotoUrl()));
        checkIn.setLegenda(vazioViraNulo(req.legenda()));
        // Troca o conteudo das colecoes (e nao a colecao) para o Hibernate acompanhar
        checkIn.getAmigosMarcados().clear();
        checkIn.getAmigosMarcados().addAll(amigos);
        checkIn.getCervejas().clear();
        checkIn.getCervejas().addAll(cervejas);

        List<CheckIn> doDesafio = checkInRepository.findDoDesafio(desafio.getId());
        return montar(doDesafio, List.of(checkIn), usuarioId).get(0);
    }

    /** So o autor apaga. Reacoes e comentarios vao junto (ON DELETE CASCADE). */
    @Transactional
    public void remover(Long checkInId, Long usuarioId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado"));

        if (!checkIn.getAutor().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Só quem fez o check-in pode apagar");
        }
        checkInRepository.delete(checkIn);
    }

    /**
     * Busca um check-in exigindo que o usuario seja do grupo do desafio (senao 403).
     * Usado por reacoes e comentarios.
     */
    @Transactional(readOnly = true)
    public CheckIn buscarVisivel(Long checkInId, Long usuarioId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado"));

        if (!checkIn.getDesafio().getGrupo().temMembro(usuarioId)) {
            throw new AcessoNegadoException("Você não faz parte do grupo deste check-in");
        }
        return checkIn;
    }

    /** Autor, ainda no grupo, com o desafio em andamento. */
    private CheckIn buscarParaEditar(Long checkInId, Long usuarioId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado"));

        if (!checkIn.getAutor().getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Só quem fez o check-in pode editar");
        }
        Challenge desafio = checkIn.getDesafio();
        if (!desafio.getGrupo().temMembro(usuarioId)) {
            throw new AcessoNegadoException("Você não faz parte do grupo deste check-in");
        }
        if (LocalDate.now(clock).isAfter(desafio.getDataFim())) {
            throw new IllegalArgumentException(
                "O desafio acabou em " + desafio.getDataFim().format(DATA) + " — os check-ins dele não mudam mais"
            );
        }
        return checkIn;
    }

    // ------------------------------------------------------------------
    // Regras
    // ------------------------------------------------------------------

    private void validarHorario(Challenge desafio, LocalDateTime feitoEm, LocalDateTime agora) {
        if (feitoEm.isAfter(agora.plus(TOLERANCIA_FUTURO))) {
            throw new IllegalArgumentException("Não dá para fazer check-in de um rolê que ainda não aconteceu");
        }
        if (feitoEm.isBefore(agora.minus(maxRetroativo))) {
            throw new IllegalArgumentException(
                "Dá para registrar rolês de até " + maxRetroativo.toHours() + "h atrás"
            );
        }
        if (feitoEm.toLocalDate().isBefore(desafio.getDataInicio())) {
            throw new IllegalArgumentException(
                "O desafio ainda não começou — começa em " + desafio.getDataInicio().format(DATA)
            );
        }
        if (feitoEm.toLocalDate().isAfter(desafio.getDataFim())) {
            throw new IllegalArgumentException(
                "O desafio acabou em " + desafio.getDataFim().format(DATA)
            );
        }
    }

    private void validarIntervalo(Long autorId, Long desafioId, LocalDateTime feitoEm) {
        checkInRepository.findConflitante(
            autorId, desafioId, feitoEm.minus(intervaloMinimo), feitoEm.plus(intervaloMinimo)
        ).ifPresent(conflito -> {
            throw new IllegalArgumentException(
                "Você já fez check-in às " + conflito.getFeitoEm().format(HORA)
                + ". Precisa de pelo menos " + descrever(intervaloMinimo) + " entre um check-in e outro."
            );
        });
    }

    private Set<User> resolverAmigos(List<Long> amigosIds, Long autorId, FriendGroup grupo) {
        Set<User> amigos = new LinkedHashSet<>();
        if (amigosIds == null) {
            return amigos;
        }
        for (Long amigoId : new LinkedHashSet<>(amigosIds)) {
            if (amigoId == null) {
                continue;
            }
            if (amigoId.equals(autorId)) {
                throw new IllegalArgumentException("Não precisa se marcar no próprio check-in");
            }
            User amigo = grupo.getMembros().stream()
                .filter(m -> m.getId().equals(amigoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Só dá para marcar quem está no grupo"));
            amigos.add(amigo);
        }
        return amigos;
    }

    private List<CervejaDoRole> resolverCervejas(List<ItemCervejaRequest> itens) {
        List<CervejaDoRole> cervejas = new ArrayList<>();
        if (itens == null || itens.isEmpty()) {
            return cervejas;
        }

        Set<Long> ids = new LinkedHashSet<>();
        for (ItemCervejaRequest item : itens) {
            if (!ids.add(item.cervejaId())) {
                throw new IllegalArgumentException("Cada cerveja entra uma vez por check-in — ajuste a quantidade dela");
            }
        }

        Map<Long, Beer> encontradas = new HashMap<>();
        beerRepository.findAllById(ids).forEach(b -> encontradas.put(b.getId(), b));
        if (encontradas.size() != ids.size()) {
            throw new RecursoNaoEncontradoException("Cerveja não encontrada no catálogo");
        }

        for (ItemCervejaRequest item : itens) {
            cervejas.add(new CervejaDoRole(encontradas.get(item.cervejaId()), item.formato(), item.quantidade()));
        }
        return cervejas;
    }

    // ------------------------------------------------------------------
    // Montagem do feed
    // ------------------------------------------------------------------

    /**
     * Monta os cards de 'alvo'. Recebe tambem todos os check-ins do desafio
     * porque os pontos de um check-in dependem do historico da pessoa.
     * Reacoes e comentarios vem em uma consulta agrupada cada, nao uma por card.
     */
    private List<CheckInResponse> montar(List<CheckIn> doDesafio, List<CheckIn> alvo, Long usuarioId) {
        if (alvo.isEmpty()) {
            return List.of();
        }
        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(doDesafio);
        List<Long> ids = alvo.stream().map(CheckIn::getId).toList();
        Map<Long, List<ReacaoResumo>> reacoes = ReacaoResumo.agrupar(
            reactionRepository.resumoPorCheckIn(ids, usuarioId)
        );
        Map<Long, Long> comentarios = contar(commentRepository.contarPorCheckIn(ids));

        return alvo.stream()
            .map(c -> CheckInResponse.de(
                c,
                pontos.get(c.getId()),
                reacoes.getOrDefault(c.getId(), List.of()),
                comentarios.getOrDefault(c.getId(), 0L)
            ))
            .toList();
    }

    private static Map<Long, Long> contar(Collection<Object[]> linhas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    /** 120 min -> "2h", 90 -> "1h30", 45 -> "45min" */
    static String descrever(Duration duracao) {
        long horas = duracao.toHours();
        long minutos = duracao.toMinutesPart();
        if (horas == 0) {
            return minutos + "min";
        }
        return minutos == 0 ? horas + "h" : horas + "h" + String.format("%02d", minutos);
    }

    private static String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

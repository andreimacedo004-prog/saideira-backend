package com.saideira.backend.service;

import com.saideira.backend.dto.AdminDtos;
import com.saideira.backend.dto.DesafioResponse;
import com.saideira.backend.dto.RankingItemResponse;
import com.saideira.backend.dto.UsuarioResumo;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.AjustePontos;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.AjustePontosRepository;
import com.saideira.backend.repository.ChallengeRepository;
import com.saideira.backend.repository.CheckInRepository;
import com.saideira.backend.repository.CommentRepository;
import com.saideira.backend.repository.FriendGroupRepository;
import com.saideira.backend.repository.UserRepository;
import com.saideira.backend.security.Administradores;
import com.saideira.backend.security.LimiteDeTentativas;
import com.saideira.backend.util.Normalizador;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Area de admin (so para as contas de APP_ADMIN_EMAILS — o SecurityConfig barra o resto).
 *
 * Tudo que o admin faz vai para o log do Railway com os ids envolvidos,
 * para ficar registrado quem mexeu em que.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    // Sem letras e numeros que se confundem (l, 1, o, 0) — a senha vai ser ditada no WhatsApp
    private static final String ALFABETO_SENHA = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_SENHA = 10;

    private final UserRepository userRepository;
    private final FriendGroupRepository friendGroupRepository;
    private final ChallengeRepository challengeRepository;
    private final CheckInRepository checkInRepository;
    private final CommentRepository commentRepository;
    private final AjustePontosRepository ajustePontosRepository;
    private final ScoreService scoreService;
    private final Administradores administradores;
    private final LimiteDeTentativas limiteDeTentativas;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public AdminService(
        UserRepository userRepository,
        FriendGroupRepository friendGroupRepository,
        ChallengeRepository challengeRepository,
        CheckInRepository checkInRepository,
        CommentRepository commentRepository,
        AjustePontosRepository ajustePontosRepository,
        ScoreService scoreService,
        Administradores administradores,
        LimiteDeTentativas limiteDeTentativas,
        PasswordEncoder passwordEncoder,
        EntityManager em,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.friendGroupRepository = friendGroupRepository;
        this.challengeRepository = challengeRepository;
        this.checkInRepository = checkInRepository;
        this.commentRepository = commentRepository;
        this.ajustePontosRepository = ajustePontosRepository;
        this.scoreService = scoreService;
        this.administradores = administradores;
        this.limiteDeTentativas = limiteDeTentativas;
        this.passwordEncoder = passwordEncoder;
        this.em = em;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Visao geral
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AdminDtos.Resumo resumo() {
        LocalDateTime seteDiasAtras = LocalDateTime.now(clock).minusDays(7);
        Long ultimaSemana = em.createQuery("SELECT COUNT(c) FROM CheckIn c WHERE c.feitoEm >= :desde", Long.class)
            .setParameter("desde", seteDiasAtras)
            .getSingleResult();
        return new AdminDtos.Resumo(
            userRepository.count(), friendGroupRepository.count(), challengeRepository.count(),
            checkInRepository.count(), ultimaSemana
        );
    }

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    /** Todas as contas, das mais novas para as mais antigas. `busca` filtra por nome ou e-mail. */
    @Transactional(readOnly = true)
    public List<AdminDtos.Usuario> usuarios(String busca) {
        Map<Long, Long> checkInsPorUsuario = new HashMap<>();
        em.createQuery("SELECT c.autor.id, COUNT(c) FROM CheckIn c GROUP BY c.autor.id", Object[].class)
            .getResultList()
            .forEach(l -> checkInsPorUsuario.put((Long) l[0], (Long) l[1]));

        Map<Long, List<String>> gruposPorUsuario = new HashMap<>();
        em.createQuery("SELECT m.id, g.nome FROM FriendGroup g JOIN g.membros m ORDER BY g.nome", Object[].class)
            .getResultList()
            .forEach(l -> gruposPorUsuario.computeIfAbsent((Long) l[0], id -> new ArrayList<>()).add((String) l[1]));

        String filtro = busca == null ? "" : Normalizador.normalizar(busca);
        return userRepository.findAll(Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"))).stream()
            .filter(u -> filtro.isEmpty()
                || Normalizador.normalizar(u.getNome()).contains(filtro)
                || u.getEmail().contains(filtro))
            .map(u -> new AdminDtos.Usuario(
                u.getId(), u.getNome(), u.getEmail(), u.getCriadoEm(),
                gruposPorUsuario.getOrDefault(u.getId(), List.of()),
                checkInsPorUsuario.getOrDefault(u.getId(), 0L),
                administradores.eAdmin(u.getEmail())
            ))
            .toList();
    }

    /** O que acontece se excluir a conta — a tela mostra antes de confirmar. */
    @Transactional(readOnly = true)
    public AdminDtos.PreviaExclusao previaExclusao(Long usuarioId, Long adminId) {
        User usuario = buscarUsuario(usuarioId);

        List<AdminDtos.Repasse> repassados = new ArrayList<>();
        List<String> apagados = new ArrayList<>();
        for (Object[] grupo : gruposCriadosPor(usuarioId)) {
            Long novoDonoId = novoDono((Long) grupo[0], usuarioId);
            if (novoDonoId == null) {
                apagados.add((String) grupo[1]);
            } else {
                repassados.add(new AdminDtos.Repasse((String) grupo[1], buscarUsuario(novoDonoId).getNome()));
            }
        }

        return new AdminDtos.PreviaExclusao(
            UsuarioResumo.de(usuario),
            contar("SELECT COUNT(*) FROM check_ins WHERE user_id = :u", usuarioId),
            contar("SELECT COUNT(*) FROM comments WHERE user_id = :u", usuarioId),
            contar("SELECT COUNT(*) FROM reactions WHERE user_id = :u", usuarioId),
            repassados,
            apagados,
            bloqueioDeExclusao(usuario, adminId)
        );
    }

    /**
     * Exclui a conta. Some junto: check-ins dela (com cervejas, amigos marcados,
     * reacoes e comentarios deles), comentarios e reacoes que ela fez, e ela
     * sai dos grupos. Grupos e desafios que ela criou passam para outro membro
     * (o de conta mais antiga); grupo em que ela estava sozinha e apagado.
     * Cervejas que ela cadastrou continuam no catalogo.
     */
    @Transactional
    public void excluirUsuario(Long usuarioId, Long adminId) {
        User usuario = buscarUsuario(usuarioId);
        String bloqueio = bloqueioDeExclusao(usuario, adminId);
        if (bloqueio != null) {
            throw new IllegalArgumentException(bloqueio);
        }

        int checkIns = executar("DELETE FROM check_ins WHERE user_id = :u", usuarioId);

        int repassados = 0;
        int apagados = 0;
        for (Object[] grupo : gruposCriadosPor(usuarioId)) {
            Long grupoId = (Long) grupo[0];
            Long novoDonoId = novoDono(grupoId, usuarioId);
            if (novoDonoId == null) {
                em.createNativeQuery("DELETE FROM friend_groups WHERE id = :g").setParameter("g", grupoId).executeUpdate();
                apagados++;
            } else {
                em.createNativeQuery("UPDATE friend_groups SET criado_por_id = :novo WHERE id = :g")
                    .setParameter("novo", novoDonoId).setParameter("g", grupoId).executeUpdate();
                repassados++;
            }
        }
        // Desafios que ela criou em grupos que ficaram: passam para o dono do grupo
        executar("""
            UPDATE challenges c SET criado_por_id = g.criado_por_id
            FROM friend_groups g
            WHERE g.id = c.group_id AND c.criado_por_id = :u
            """, usuarioId);

        executar("DELETE FROM users WHERE id = :u", usuarioId);
        em.flush();
        em.clear();

        log.info("Admin {} excluiu a conta {} ({} check-ins, {} grupos repassados, {} grupos apagados)",
            adminId, usuarioId, checkIns, repassados, apagados);
    }

    /**
     * Gera uma senha nova para quem esqueceu a dela. Devolve a senha uma vez
     * so, para o admin passar para a pessoa; o banco guarda apenas o hash.
     */
    @Transactional
    public AdminDtos.SenhaTemporaria gerarSenhaTemporaria(Long usuarioId, Long adminId) {
        User usuario = buscarUsuario(usuarioId);
        if (administradores.eAdmin(usuario.getEmail()) && !usuario.getId().equals(adminId)) {
            throw new IllegalArgumentException("A senha de outra conta de admin não muda por aqui");
        }
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            senha.append(ALFABETO_SENHA.charAt(aleatorio.nextInt(ALFABETO_SENHA.length())));
        }
        usuario.setSenhaHash(passwordEncoder.encode(senha.toString()));
        limiteDeTentativas.liberar(usuario.getEmail());

        log.info("Admin {} gerou senha temporaria para a conta {}", adminId, usuarioId);
        return new AdminDtos.SenhaTemporaria(senha.toString());
    }

    // ------------------------------------------------------------------
    // Desafios e pontos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminDtos.Desafio> desafios() {
        Map<Long, Long> checkInsPorDesafio = new HashMap<>();
        em.createQuery("SELECT c.desafio.id, COUNT(c) FROM CheckIn c GROUP BY c.desafio.id", Object[].class)
            .getResultList()
            .forEach(l -> checkInsPorDesafio.put((Long) l[0], (Long) l[1]));

        LocalDate hoje = LocalDate.now(clock);
        return challengeRepository.findAll(Sort.by(Sort.Order.desc("dataFim"), Sort.Order.desc("id"))).stream()
            .map(c -> {
                DesafioResponse d = DesafioResponse.de(c, hoje);
                return new AdminDtos.Desafio(
                    c.getId(), c.getNome(), c.getGrupo().getNome(), d.status(), c.getDataInicio(), c.getDataFim(),
                    c.getGrupo().getMembros().size(), checkInsPorDesafio.getOrDefault(c.getId(), 0L)
                );
            })
            .toList();
    }

    /** Ranking, check-ins e ajustes de um desafio — o admin ve sem precisar ser do grupo. */
    @Transactional(readOnly = true)
    public AdminDtos.DesafioDetalhe desafio(Long desafioId) {
        Challenge desafio = buscarDesafio(desafioId);
        List<CheckIn> checkIns = checkInRepository.findDoDesafio(desafioId);
        List<AjustePontos> ajustes = ajustePontosRepository.findDoDesafio(desafioId);

        List<RankingItemResponse> ranking = scoreService.ranking(desafio.getGrupo().getMembros(), checkIns, ajustes)
            .stream().map(RankingItemResponse::de).toList();

        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(checkIns);
        Map<Long, Long> comentarios = new HashMap<>();
        if (!checkIns.isEmpty()) {
            commentRepository.contarPorCheckIn(checkIns.stream().map(CheckIn::getId).toList())
                .forEach(l -> comentarios.put((Long) l[0], (Long) l[1]));
        }
        List<AdminDtos.CheckInLinha> linhas = checkIns.stream()
            .map(c -> new AdminDtos.CheckInLinha(
                c.getId(), UsuarioResumo.de(c.getAutor()), c.getLocal(), c.getTipo(), c.getFeitoEm(),
                pontos.get(c.getId()).total(), comentarios.getOrDefault(c.getId(), 0L)
            ))
            .toList();

        // Quem pode receber ajuste: membros do grupo e quem ja fez check-in no desafio
        Map<Long, User> participantes = new LinkedHashMap<>();
        desafio.getGrupo().getMembros().forEach(u -> participantes.put(u.getId(), u));
        checkIns.forEach(c -> participantes.putIfAbsent(c.getAutor().getId(), c.getAutor()));

        return new AdminDtos.DesafioDetalhe(
            DesafioResponse.de(desafio, LocalDate.now(clock)),
            ranking,
            linhas,
            ajustes.stream().map(AdminService::paraDto).toList(),
            participantes.values().stream()
                .map(UsuarioResumo::de)
                .sorted(Comparator.comparing(UsuarioResumo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList()
        );
    }

    @Transactional
    public AdminDtos.Ajuste ajustarPontos(Long desafioId, Long adminId, AdminDtos.NovoAjuste pedido) {
        Challenge desafio = buscarDesafio(desafioId);
        if (pedido.pontos() == 0) {
            throw new IllegalArgumentException("Um ajuste precisa somar ou tirar pelo menos 1 ponto");
        }
        User usuario = buscarUsuario(pedido.usuarioId());
        boolean fezCheckIn = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM check_ins WHERE user_id = :u AND challenge_id = :d")
            .setParameter("u", usuario.getId()).setParameter("d", desafio.getId())
            .getSingleResult()).longValue() > 0;
        boolean participa = desafio.getGrupo().temMembro(usuario.getId()) || fezCheckIn;
        if (!participa) {
            throw new IllegalArgumentException("Essa pessoa não participa deste desafio");
        }

        AjustePontos ajuste = new AjustePontos();
        ajuste.setDesafio(desafio);
        ajuste.setUsuario(usuario);
        ajuste.setPontos(pedido.pontos());
        ajuste.setMotivo(pedido.motivo().trim());
        ajuste.setCriadoPor(userRepository.getReferenceById(adminId));
        ajuste.setCriadoEm(LocalDateTime.now(clock));
        ajuste = ajustePontosRepository.save(ajuste);

        log.info("Admin {} ajustou {} pontos da conta {} no desafio {}", adminId, pedido.pontos(), usuario.getId(), desafioId);
        return paraDto(ajuste);
    }

    @Transactional
    public void removerAjuste(Long ajusteId, Long adminId) {
        AjustePontos ajuste = ajustePontosRepository.findById(ajusteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ajuste não encontrado"));
        ajustePontosRepository.delete(ajuste);
        log.info("Admin {} desfez o ajuste {}", adminId, ajusteId);
    }

    /** Apaga o check-in de qualquer pessoa (reacoes e comentarios vao junto). */
    @Transactional
    public void apagarCheckIn(Long checkInId, Long adminId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Check-in não encontrado"));
        checkInRepository.delete(checkIn);
        log.info("Admin {} apagou o check-in {}", adminId, checkInId);
    }

    /** Apaga o comentario de qualquer pessoa. */
    @Transactional
    public void apagarComentario(Long comentarioId, Long adminId) {
        var comentario = commentRepository.findById(comentarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado"));
        commentRepository.delete(comentario);
        log.info("Admin {} apagou o comentario {}", adminId, comentarioId);
    }

    // ------------------------------------------------------------------

    private String bloqueioDeExclusao(User usuario, Long adminId) {
        if (usuario.getId().equals(adminId)) {
            return "Você não pode excluir a própria conta por aqui";
        }
        if (administradores.eAdmin(usuario.getEmail())) {
            return "Contas de admin não podem ser excluídas por aqui";
        }
        return null;
    }

    /** [id, nome] dos grupos que a pessoa criou. */
    @SuppressWarnings("unchecked")
    private List<Object[]> gruposCriadosPor(Long usuarioId) {
        List<Object[]> linhas = em.createNativeQuery("SELECT id, nome FROM friend_groups WHERE criado_por_id = :u ORDER BY id")
            .setParameter("u", usuarioId)
            .getResultList();
        return linhas.stream().map(l -> new Object[] {((Number) l[0]).longValue(), l[1]}).toList();
    }

    /** Outro membro do grupo que herda: o de conta mais antiga. Nulo se nao sobrar ninguem. */
    private Long novoDono(Long grupoId, Long saindoId) {
        Object resultado = em.createNativeQuery(
                "SELECT MIN(user_id) FROM friend_group_members WHERE group_id = :g AND user_id <> :u")
            .setParameter("g", grupoId).setParameter("u", saindoId)
            .getSingleResult();
        return resultado == null ? null : ((Number) resultado).longValue();
    }

    private long contar(String sql, Long usuarioId) {
        return ((Number) em.createNativeQuery(sql).setParameter("u", usuarioId).getSingleResult()).longValue();
    }

    private int executar(String sql, Long usuarioId) {
        return em.createNativeQuery(sql).setParameter("u", usuarioId).executeUpdate();
    }

    private User buscarUsuario(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private Challenge buscarDesafio(Long id) {
        return challengeRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Desafio não encontrado"));
    }

    private static AdminDtos.Ajuste paraDto(AjustePontos a) {
        return new AdminDtos.Ajuste(
            a.getId(), UsuarioResumo.de(a.getUsuario()), a.getPontos(), a.getMotivo(), a.getCriadoEm(),
            a.getCriadoPor() == null ? null : a.getCriadoPor().getNome()
        );
    }
}

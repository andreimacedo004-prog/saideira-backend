package com.saideira.backend.service;

import com.saideira.backend.dto.FriendGroupResponse;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.FriendGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Grupos de amigos: criar, entrar pelo convite e consultar.
 *
 * Os metodos publicos devolvem DTO, nao entidade. Com open-in-view=false
 * a sessao do Hibernate fecha ao sair do service, e montar o DTO no
 * controller estouraria LazyInitializationException ao ler os membros.
 */
@Service
public class FriendGroupService {

    private static final Logger log = LoggerFactory.getLogger(FriendGroupService.class);

    // Sem vogais e sem caracteres ambiguos (0/O, 1/l) — codigo facil de ditar
    private static final String ALFABETO = "23456789bcdfghjkmnpqrstvwxz";
    private static final int TAMANHO_CODIGO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FriendGroupRepository friendGroupRepository;
    private final UserService userService;

    public FriendGroupService(FriendGroupRepository friendGroupRepository, UserService userService) {
        this.friendGroupRepository = friendGroupRepository;
        this.userService = userService;
    }

    @Transactional
    public FriendGroupResponse criar(Long criadorId, String nome) {
        User criador = userService.buscarPorId(criadorId);

        FriendGroup grupo = new FriendGroup();
        grupo.setNome(nome.trim());
        grupo.setCriadoPor(criador);
        grupo.setCodigoConvite(gerarCodigoUnico());
        grupo.getMembros().add(criador);

        return FriendGroupResponse.de(friendGroupRepository.save(grupo));
    }

    /** Entrada pelo link de convite. Entrar duas vezes e inofensivo — so devolve o grupo. */
    @Transactional
    public FriendGroupResponse entrarPorCodigo(String codigoConvite, Long usuarioId) {
        FriendGroup grupo = friendGroupRepository.findByCodigoConvite(codigoConvite.trim().toLowerCase())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Convite inválido ou expirado"));

        if (!grupo.temMembro(usuarioId)) {
            grupo.getMembros().add(userService.buscarPorId(usuarioId));
            grupo = friendGroupRepository.save(grupo);
        }
        return FriendGroupResponse.de(grupo);
    }

    /**
     * Muda so o nome. Pode quem criou o grupo ou o admin (mesmo sem ser membro).
     * Quem nao e do grupo recebe o mesmo 403 de sempre, sem saber quem criou.
     */
    @Transactional
    public FriendGroupResponse renomear(Long grupoId, Long usuarioId, boolean admin, String nome) {
        FriendGroup grupo = friendGroupRepository.findById(grupoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Grupo não encontrado"));

        boolean criador = grupo.getCriadoPor().getId().equals(usuarioId);
        if (!criador && !admin) {
            if (!grupo.temMembro(usuarioId)) {
                throw new AcessoNegadoException("Você não faz parte deste grupo");
            }
            throw new AcessoNegadoException("Só quem criou o grupo pode mudar o nome");
        }

        grupo.setNome(nome.trim());
        if (!criador) {
            log.info("Admin {} renomeou o grupo {}", usuarioId, grupoId);
        }
        return FriendGroupResponse.de(grupo);
    }

    @Transactional(readOnly = true)
    public List<FriendGroupResponse> gruposDoUsuario(Long usuarioId) {
        return friendGroupRepository.findByMembrosId(usuarioId).stream()
            .map(FriendGroupResponse::de)
            .toList();
    }

    @Transactional(readOnly = true)
    public FriendGroupResponse detalhe(Long grupoId, Long usuarioId) {
        return FriendGroupResponse.de(buscarGrupoDoMembro(grupoId, usuarioId));
    }

    @Transactional(readOnly = true)
    public String codigoConvite(Long grupoId, Long usuarioId) {
        return buscarGrupoDoMembro(grupoId, usuarioId).getCodigoConvite();
    }

    /**
     * Busca um grupo exigindo que o usuario seja membro dele (senao 403).
     * Uso interno dos services: devolve a entidade.
     */
    @Transactional(readOnly = true)
    public FriendGroup buscarGrupoDoMembro(Long grupoId, Long usuarioId) {
        FriendGroup grupo = friendGroupRepository.findById(grupoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Grupo não encontrado"));

        if (!grupo.temMembro(usuarioId)) {
            throw new AcessoNegadoException("Você não faz parte deste grupo");
        }
        return grupo;
    }

    private String gerarCodigoUnico() {
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            }
            String codigo = sb.toString();
            if (!friendGroupRepository.existsByCodigoConvite(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de convite único");
    }
}

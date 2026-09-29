package com.saideira.backend.service;

import com.saideira.backend.dto.DesafioResponse;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.AjustePontosRepository;
import com.saideira.backend.repository.ChallengeRepository;
import com.saideira.backend.repository.CheckInRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Editar nome e apagar desafio: so quem criou, e so dentro do grupo. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChallengeServiceTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Mock private ChallengeRepository challengeRepository;
    @Mock private CheckInRepository checkInRepository;
    @Mock private AjustePontosRepository ajustePontosRepository;
    @Mock private FriendGroupService friendGroupService;
    @Mock private UserService userService;

    private ChallengeService service;
    private User ana;
    private User bia;
    private Challenge desafio;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(LocalDate.of(2026, 11, 7).atStartOfDay(BRASILIA).toInstant(), BRASILIA);
        service = new ChallengeService(
            challengeRepository, checkInRepository, ajustePontosRepository, friendGroupService, userService,
            new ScoreService(), relogio
        );

        ana = usuario(1L, "Ana");
        bia = usuario(2L, "Bia");

        FriendGroup grupo = new FriendGroup();
        grupo.setId(10L);
        grupo.setNome("Resenha");
        grupo.setCriadoPor(ana);
        grupo.getMembros().add(ana);
        grupo.getMembros().add(bia);

        desafio = new Challenge();
        desafio.setId(100L);
        desafio.setGrupo(grupo);
        desafio.setNome("Rolês de Fim de Ano");
        desafio.setDataInicio(LocalDate.of(2026, 11, 1));
        desafio.setDataFim(LocalDate.of(2026, 12, 31));
        desafio.setCriadoPor(ana);

        when(challengeRepository.findById(100L)).thenReturn(Optional.of(desafio));
    }

    @Test
    @DisplayName("Quem criou muda o nome; datas e status ficam como estavam")
    void criadorRenomeia() {
        DesafioResponse resposta = service.renomear(100L, ana.getId(), "Saideira 2026");

        assertThat(desafio.getNome()).isEqualTo("Saideira 2026");
        assertThat(resposta.nome()).isEqualTo("Saideira 2026");
        assertThat(resposta.dataInicio()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(resposta.dataFim()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(resposta.status()).isEqualTo(DesafioResponse.Status.ATIVO);
        assertThat(resposta.criadoPorId()).isEqualTo(ana.getId());
    }

    @Test
    @DisplayName("Outro membro do grupo nao muda o nome")
    void membroNaoRenomeia() {
        assertThatThrownBy(() -> service.renomear(100L, bia.getId(), "Nome da Bia"))
            .isInstanceOf(AcessoNegadoException.class)
            .hasMessageContaining("Só quem criou o desafio pode mudar o nome");

        assertThat(desafio.getNome()).isEqualTo("Rolês de Fim de Ano");
    }

    @Test
    @DisplayName("Quem criou apaga o desafio")
    void criadorApaga() {
        service.apagar(100L, ana.getId());

        verify(challengeRepository).delete(desafio);
    }

    @Test
    @DisplayName("Outro membro do grupo nao apaga")
    void membroNaoApaga() {
        assertThatThrownBy(() -> service.apagar(100L, bia.getId()))
            .isInstanceOf(AcessoNegadoException.class)
            .hasMessageContaining("Só quem criou o desafio pode apagar");

        verify(challengeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Quem nao e do grupo nao apaga nem renomeia (nem descobre quem criou)")
    void deForaNaoMexe() {
        Long forasteiro = 99L;

        assertThatThrownBy(() -> service.apagar(100L, forasteiro))
            .isInstanceOf(AcessoNegadoException.class)
            .hasMessageContaining("não faz parte do grupo");
        assertThatThrownBy(() -> service.renomear(100L, forasteiro, "Invasão"))
            .isInstanceOf(AcessoNegadoException.class);

        verify(challengeRepository, never()).delete(any());
        assertThat(desafio.getNome()).isEqualTo("Rolês de Fim de Ano");
    }

    @Test
    @DisplayName("Desafio que nao existe da 404")
    void desafioInexistente() {
        when(challengeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apagar(404L, ana.getId()))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.renomear(404L, ana.getId(), "X"))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Nome chega sem espacos sobrando")
    void nomeSemEspacos() {
        service.renomear(100L, ana.getId(), "  Saideira  ");

        assertThat(desafio.getNome()).isEqualTo("Saideira");
    }

    private static User usuario(Long id, String nome) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@teste.dev");
        return u;
    }
}

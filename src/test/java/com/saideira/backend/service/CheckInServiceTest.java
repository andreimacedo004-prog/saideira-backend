package com.saideira.backend.service;

import com.saideira.backend.dto.CheckInResponse;
import com.saideira.backend.dto.RegistrarCheckInRequest;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.Beer;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FriendGroup;
import com.saideira.backend.model.User;
import com.saideira.backend.repository.BeerRepository;
import com.saideira.backend.repository.CheckInRepository;
import com.saideira.backend.repository.CommentRepository;
import com.saideira.backend.repository.ReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckInServiceTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    // "Agora" congelado: sabado, 07/11/2026, 23h em Brasilia
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 7, 23, 0);

    @Mock private CheckInRepository checkInRepository;
    @Mock private ChallengeService challengeService;
    @Mock private BeerRepository beerRepository;
    @Mock private UserService userService;
    @Mock private ReactionRepository reactionRepository;
    @Mock private CommentRepository commentRepository;

    private CheckInService service;
    private CheckIn salvo;

    private User ana;
    private User bia;
    private User forasteiro;
    private Challenge desafio;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(AGORA.atZone(BRASILIA).toInstant(), BRASILIA);
        service = new CheckInService(
            checkInRepository, challengeService, beerRepository, userService,
            reactionRepository, commentRepository, new ScoreService(), relogio, 120, 24
        );

        ana = usuario(1L, "Ana");
        bia = usuario(2L, "Bia");
        forasteiro = usuario(99L, "Forasteiro");

        FriendGroup grupo = new FriendGroup();
        grupo.setId(10L);
        grupo.getMembros().addAll(List.of(ana, bia));

        desafio = new Challenge();
        desafio.setId(100L);
        desafio.setGrupo(grupo);
        desafio.setDataInicio(LocalDate.of(2026, 11, 1));
        desafio.setDataFim(LocalDate.of(2026, 12, 31));

        when(challengeService.buscarDoMembro(100L, 1L)).thenReturn(desafio);
        when(userService.buscarPorId(1L)).thenReturn(ana);
        when(checkInRepository.findConflitante(anyLong(), anyLong(), any(), any())).thenReturn(Optional.empty());
        // Simula o banco: o que foi salvo passa a aparecer na consulta do desafio
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> {
            salvo = inv.getArgument(0);
            salvo.setId(500L);
            return salvo;
        });
        when(checkInRepository.findDoDesafio(100L)).thenAnswer(inv -> salvo == null ? List.of() : List.of(salvo));
        when(reactionRepository.resumoPorCheckIn(any(), any())).thenReturn(List.of());
        when(commentRepository.contarPorCheckIn(any())).thenReturn(List.of());
    }

    private User usuario(Long id, String nome) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        return u;
    }

    private RegistrarCheckInRequest pedido(LocalDateTime feitoEm, List<Long> amigos, List<Long> cervejas) {
        return new RegistrarCheckInRequest(
            CheckIn.TipoRole.BAR, "Bar do Zé", null, "Saideira!", feitoEm, amigos, cervejas
        );
    }

    @Test
    @DisplayName("Check-in valido grava com horario de agora, amigo marcado e pontos calculados")
    void registraCheckInValido() {
        Beer heineken = new Beer();
        heineken.setId(7L);
        heineken.setNome("Heineken");
        when(beerRepository.findAllById(any())).thenReturn(List.of(heineken));
        CheckInResponse resposta = service.registrar(1L, 100L, pedido(null, List.of(2L), List.of(7L)));

        assertThat(resposta.feitoEm()).isEqualTo(AGORA);
        assertThat(resposta.amigos()).extracting(a -> a.nome()).containsExactly("Bia");
        // 10 + cerveja nova (5) + 1 amigo (3) + lugar novo (5)
        assertThat(resposta.pontos().total()).isEqualTo(23);
    }

    @Test
    @DisplayName("Segundo check-in antes de 2h e barrado, com o horario do anterior na mensagem")
    void barraIntervaloMinimo() {
        CheckIn anterior = new CheckIn();
        anterior.setFeitoEm(AGORA.minusMinutes(50));
        when(checkInRepository.findConflitante(eq(1L), eq(100L), any(), any())).thenReturn(Optional.of(anterior));

        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("22:10")
            .hasMessageContaining("2h");
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("A janela do intervalo e de 2h para cada lado do horario do role")
    void janelaDoIntervalo() {
        service.registrar(1L, 100L, pedido(null, null, null));

        verify(checkInRepository).findConflitante(1L, 100L, AGORA.minusHours(2), AGORA.plusHours(2));
    }

    @Test
    @DisplayName("Check-in no futuro e recusado (alem da tolerancia de 5 min)")
    void recusaFuturo() {
        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(AGORA.plusMinutes(30), null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ainda nao aconteceu");
    }

    @Test
    @DisplayName("Retroativo de mais de 24h e recusado; de 20h atras e aceito")
    void limiteRetroativo() {
        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(AGORA.minusHours(25), null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("24h");

        CheckInResponse resposta = service.registrar(1L, 100L, pedido(AGORA.minusHours(20), null, null));
        assertThat(resposta.feitoEm()).isEqualTo(AGORA.minusHours(20));
    }

    @Test
    @DisplayName("Role fora do periodo do desafio nao conta")
    void foraDoPeriodo() {
        desafio.setDataInicio(LocalDate.of(2026, 11, 8)); // comeca amanha

        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ainda nao comecou");

        desafio.setDataInicio(LocalDate.of(2026, 11, 1));
        desafio.setDataFim(LocalDate.of(2026, 11, 6)); // acabou ontem

        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("acabou");
    }

    @Test
    @DisplayName("Nao da para marcar quem nao esta no grupo, nem a si mesmo")
    void regrasDeMarcacao() {
        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, List.of(forasteiro.getId()), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grupo");

        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, List.of(1L), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("se marcar");
    }

    @Test
    @DisplayName("Cerveja que nao existe no catalogo vira 404")
    void cervejaInexistente() {
        when(beerRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.registrar(1L, 100L, pedido(null, null, List.of(12345L))))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("So o autor apaga o check-in")
    void soAutorApaga() {
        CheckIn daAna = new CheckIn();
        daAna.setId(500L);
        daAna.setAutor(ana);
        when(checkInRepository.findById(500L)).thenReturn(Optional.of(daAna));

        assertThatThrownBy(() -> service.remover(500L, 2L)).isInstanceOf(AcessoNegadoException.class);

        service.remover(500L, 1L);
        verify(checkInRepository).delete(daAna);
    }

    @Test
    @DisplayName("Quem nao e do grupo nao ve o check-in")
    void naoMembroNaoVe() {
        CheckIn daAna = new CheckIn();
        daAna.setId(500L);
        daAna.setAutor(ana);
        daAna.setDesafio(desafio);
        when(checkInRepository.findById(500L)).thenReturn(Optional.of(daAna));

        assertThatThrownBy(() -> service.buscarVisivel(500L, forasteiro.getId()))
            .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("Descricao do intervalo para a mensagem de erro")
    void descreveIntervalo() {
        assertThat(CheckInService.descrever(Duration.ofMinutes(120))).isEqualTo("2h");
        assertThat(CheckInService.descrever(Duration.ofMinutes(90))).isEqualTo("1h30");
        assertThat(CheckInService.descrever(Duration.ofMinutes(45))).isEqualTo("45min");
    }
}

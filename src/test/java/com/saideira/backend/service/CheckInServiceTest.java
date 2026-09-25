package com.saideira.backend.service;

import com.saideira.backend.dto.CheckInParaEditarResponse;
import com.saideira.backend.dto.CheckInResponse;
import com.saideira.backend.dto.EditarCheckInRequest;
import com.saideira.backend.dto.ItemCervejaRequest;
import com.saideira.backend.dto.RegistrarCheckInRequest;
import com.saideira.backend.exception.AcessoNegadoException;
import com.saideira.backend.exception.RecursoNaoEncontradoException;
import com.saideira.backend.model.Beer;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.FormatoCerveja;
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
import static org.mockito.Mockito.times;
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

    private RegistrarCheckInRequest pedido(LocalDateTime feitoEm, List<Long> amigos, List<Long> cervejaIds) {
        List<ItemCervejaRequest> cervejas = cervejaIds == null ? null
            : cervejaIds.stream().map(id -> new ItemCervejaRequest(id, FormatoCerveja.LATA, 1)).toList();
        return pedidoComCervejas(feitoEm, amigos, cervejas);
    }

    private RegistrarCheckInRequest pedidoComCervejas(LocalDateTime feitoEm, List<Long> amigos, List<ItemCervejaRequest> cervejas) {
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
            .hasMessageContaining("ainda não aconteceu");
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
            .hasMessageContaining("ainda não começou");

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
    @DisplayName("Formato e quantidade ficam gravados no check-in (para a retrospectiva)")
    void gravaFormatoEQuantidade() {
        Beer heineken = new Beer();
        heineken.setId(7L);
        heineken.setNome("Heineken");
        when(beerRepository.findAllById(any())).thenReturn(List.of(heineken));

        CheckInResponse resposta = service.registrar(1L, 100L,
            pedidoComCervejas(null, null, List.of(new ItemCervejaRequest(7L, FormatoCerveja.GARRAFA, 3))));

        assertThat(salvo.getCervejas()).singleElement().satisfies(item -> {
            assertThat(item.getFormato()).isEqualTo(FormatoCerveja.GARRAFA);
            assertThat(item.getQuantidade()).isEqualTo(3);
            assertThat(item.mililitros()).isEqualTo(1800);
        });
        // 3 garrafas continuam valendo so +5 (uma cerveja nova)
        assertThat(resposta.pontos().total()).isEqualTo(10 + 5 + 5);
    }

    @Test
    @DisplayName("A mesma cerveja duas vezes no mesmo check-in e recusada")
    void cervejaRepetidaNoCheckIn() {
        assertThatThrownBy(() -> service.registrar(1L, 100L, pedidoComCervejas(null, null, List.of(
            new ItemCervejaRequest(7L, FormatoCerveja.LATA, 1),
            new ItemCervejaRequest(7L, FormatoCerveja.GARRAFA, 1)
        ))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uma vez por check-in");
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

    // ------------------------------------------------------------------
    // Editar
    // ------------------------------------------------------------------

    /** Check-in da Ana ja gravado: Bar do Ze, sem cerveja nem amigo (10 + 5 de lugar novo). */
    private CheckIn checkInDaAnaGravado() {
        service.registrar(1L, 100L, pedido(AGORA.minusHours(3), null, null));
        salvo.setDesafio(desafio);
        when(checkInRepository.findById(500L)).thenReturn(Optional.of(salvo));
        return salvo;
    }

    private Beer heineken() {
        Beer b = new Beer();
        b.setId(7L);
        b.setNome("Heineken");
        return b;
    }

    @Test
    @DisplayName("Autor edita tudo menos o horario, e os pontos saem recalculados")
    void autorEdita() {
        CheckIn checkIn = checkInDaAnaGravado();
        when(beerRepository.findAllById(any())).thenReturn(List.of(heineken()));

        CheckInResponse resposta = service.editar(500L, 1L, new EditarCheckInRequest(
            CheckIn.TipoRole.FESTA, "  Casa da Bia ", "https://res.cloudinary.com/x/foto.jpg", "   ",
            List.of(2L), List.of(new ItemCervejaRequest(7L, FormatoCerveja.GARRAFA, 2))
        ));

        assertThat(checkIn.getTipo()).isEqualTo(CheckIn.TipoRole.FESTA);
        assertThat(checkIn.getLocal()).isEqualTo("Casa da Bia");
        assertThat(checkIn.getLocalNormalizado()).isEqualTo("casa da bia");
        assertThat(checkIn.getFotoUrl()).isEqualTo("https://res.cloudinary.com/x/foto.jpg");
        assertThat(checkIn.getLegenda()).isNull();
        assertThat(checkIn.getAmigosMarcados()).containsExactly(bia);
        assertThat(checkIn.getCervejas()).singleElement().satisfies(item -> {
            assertThat(item.getFormato()).isEqualTo(FormatoCerveja.GARRAFA);
            assertThat(item.getQuantidade()).isEqualTo(2);
        });
        assertThat(resposta.feitoEm()).isEqualTo(AGORA.minusHours(3));
        // 10 + cerveja nova (5) + 1 amigo (3) + lugar novo (5)
        assertThat(resposta.pontos().total()).isEqualTo(23);
        // So o registro consultou o intervalo: editar nao mexe no horario, entao nao reconfere
        verify(checkInRepository, times(1)).findConflitante(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Editar pode tirar foto, amigos e cervejas")
    void editarTiraTudo() {
        CheckIn checkIn = checkInDaAnaGravado();
        checkIn.setFotoUrl("https://res.cloudinary.com/x/antiga.jpg");
        checkIn.getAmigosMarcados().add(bia);

        service.editar(500L, 1L, new EditarCheckInRequest(
            CheckIn.TipoRole.BAR, "Bar do Zé", "", null, List.of(), null
        ));

        assertThat(checkIn.getFotoUrl()).isNull();
        assertThat(checkIn.getAmigosMarcados()).isEmpty();
        assertThat(checkIn.getCervejas()).isEmpty();
    }

    @Test
    @DisplayName("So o autor edita (e so ele recebe os dados de edicao)")
    void soAutorEdita() {
        CheckIn checkIn = checkInDaAnaGravado();
        EditarCheckInRequest pedido = new EditarCheckInRequest(CheckIn.TipoRole.SHOW, "Outro lugar", null, null, null, null);

        assertThatThrownBy(() -> service.editar(500L, 2L, pedido))
            .isInstanceOf(AcessoNegadoException.class)
            .hasMessageContaining("Só quem fez o check-in pode editar");
        assertThatThrownBy(() -> service.paraEditar(500L, 2L))
            .isInstanceOf(AcessoNegadoException.class);

        assertThat(checkIn.getLocal()).isEqualTo("Bar do Zé");
        assertThat(checkIn.getTipo()).isEqualTo(CheckIn.TipoRole.BAR);
    }

    @Test
    @DisplayName("Depois que o desafio acaba, ninguem edita")
    void desafioEncerradoNaoEdita() {
        CheckIn checkIn = checkInDaAnaGravado();
        desafio.setDataFim(LocalDate.of(2026, 11, 6)); // acabou ontem

        assertThatThrownBy(() -> service.editar(500L, 1L,
            new EditarCheckInRequest(CheckIn.TipoRole.BAR, "Outro", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("não mudam mais");
        assertThat(checkIn.getLocal()).isEqualTo("Bar do Zé");
    }

    @Test
    @DisplayName("Edicao invalida nao muda nada no check-in")
    void edicaoInvalidaNaoMexe() {
        CheckIn checkIn = checkInDaAnaGravado();

        assertThatThrownBy(() -> service.editar(500L, 1L, new EditarCheckInRequest(
            CheckIn.TipoRole.FESTA, "Outro lugar", null, null, List.of(forasteiro.getId()), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grupo");
        assertThatThrownBy(() -> service.editar(500L, 1L, new EditarCheckInRequest(
            CheckIn.TipoRole.FESTA, "Outro lugar", null, null, List.of(1L), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("se marcar");
        assertThatThrownBy(() -> service.editar(500L, 1L, new EditarCheckInRequest(
            CheckIn.TipoRole.FESTA, "Outro lugar", null, null, null, List.of(
                new ItemCervejaRequest(7L, FormatoCerveja.LATA, 1), new ItemCervejaRequest(7L, FormatoCerveja.LATA, 2)))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(checkIn.getLocal()).isEqualTo("Bar do Zé");
        assertThat(checkIn.getTipo()).isEqualTo(CheckIn.TipoRole.BAR);
    }

    @Test
    @DisplayName("Dados de edicao trazem formato, quantidade e ids dos amigos")
    void dadosParaEditar() {
        when(beerRepository.findAllById(any())).thenReturn(List.of(heineken()));
        service.registrar(1L, 100L, pedidoComCervejas(null, List.of(2L),
            List.of(new ItemCervejaRequest(7L, FormatoCerveja.LATAO, 4))));
        salvo.setDesafio(desafio);
        when(checkInRepository.findById(500L)).thenReturn(Optional.of(salvo));

        CheckInParaEditarResponse dados = service.paraEditar(500L, 1L);

        assertThat(dados.amigosIds()).containsExactly(2L);
        assertThat(dados.cervejas()).singleElement().satisfies(item -> {
            assertThat(item.cerveja().nome()).isEqualTo("Heineken");
            assertThat(item.formato()).isEqualTo(FormatoCerveja.LATAO);
            assertThat(item.quantidade()).isEqualTo(4);
        });
        assertThat(dados.local()).isEqualTo("Bar do Zé");
        assertThat(dados.legenda()).isEqualTo("Saideira!");
    }

    @Test
    @DisplayName("Descricao do intervalo para a mensagem de erro")
    void descreveIntervalo() {
        assertThat(CheckInService.descrever(Duration.ofMinutes(120))).isEqualTo("2h");
        assertThat(CheckInService.descrever(Duration.ofMinutes(90))).isEqualTo("1h30");
        assertThat(CheckInService.descrever(Duration.ofMinutes(45))).isEqualTo("45min");
    }
}

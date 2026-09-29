package com.saideira.backend.service;

import com.saideira.backend.model.Beer;
import com.saideira.backend.model.CervejaDoRole;
import com.saideira.backend.model.FormatoCerveja;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.User;
import com.saideira.backend.util.Normalizador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreServiceTest {

    private final ScoreService scoreService = new ScoreService();

    private static final LocalDateTime SEXTA = LocalDateTime.of(2026, 11, 6, 22, 0);

    private long proximoId = 1;

    private User usuario(long id, String nome) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        return u;
    }

    private Beer cerveja(long id) {
        Beer b = new Beer();
        b.setId(id);
        b.setNome("Cerveja " + id);
        return b;
    }

    private CheckIn checkIn(User autor, String local, LocalDateTime feitoEm, List<Beer> cervejas, List<User> amigos) {
        CheckIn c = new CheckIn();
        c.setId(proximoId++);
        c.setAutor(autor);
        c.setLocal(local);
        c.setLocalNormalizado(Normalizador.normalizar(local));
        c.setFeitoEm(feitoEm);
        cervejas.forEach(b -> c.getCervejas().add(new CervejaDoRole(b, FormatoCerveja.LATA, 1)));
        c.getAmigosMarcados().addAll(amigos);
        return c;
    }

    @Test
    @DisplayName("Primeiro check-in vale 10 + 5 de lugar novo")
    void checkInSimples() {
        User ana = usuario(1, "Ana");
        CheckIn c = checkIn(ana, "Bar do Zé", SEXTA, List.of(), List.of());

        ScoreService.PontosCheckIn pontos = scoreService.pontuarCheckIns(List.of(c)).get(c.getId());

        assertThat(pontos.total()).isEqualTo(15);
        assertThat(pontos.lugarNovo()).isTrue();
    }

    @Test
    @DisplayName("Tudo junto: 10 + 2 cervejas novas (10) + 3 amigos (9) + lugar novo (5) = 34")
    void checkInCompleto() {
        User ana = usuario(1, "Ana");
        CheckIn c = checkIn(ana, "Bar do Zé", SEXTA,
            List.of(cerveja(1), cerveja(2)),
            List.of(usuario(2, "Bia"), usuario(3, "Caio"), usuario(4, "Duda")));

        ScoreService.PontosCheckIn pontos = scoreService.pontuarCheckIns(List.of(c)).get(c.getId());

        assertThat(pontos.total()).isEqualTo(34);
        assertThat(pontos.cervejasNovas()).isEqualTo(2);
        assertThat(pontos.amigosMarcados()).isEqualTo(3);
    }

    @Test
    @DisplayName("Cerveja repetida nao pontua de novo; so a nova ganha bonus")
    void cervejaRepetida() {
        User ana = usuario(1, "Ana");
        CheckIn primeiro = checkIn(ana, "Bar A", SEXTA, List.of(cerveja(1)), List.of());
        CheckIn segundo = checkIn(ana, "Bar B", SEXTA.plusDays(1), List.of(cerveja(1), cerveja(2)), List.of());

        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(List.of(primeiro, segundo));

        assertThat(pontos.get(primeiro.getId()).cervejasNovas()).isEqualTo(1);
        assertThat(pontos.get(segundo.getId()).cervejasNovas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Quantidade e formato nao mudam os pontos: 6 latoes da mesma cerveja = 1 cerveja nova")
    void quantidadeNaoPontua() {
        User ana = usuario(1, "Ana");
        CheckIn umaLata = checkIn(ana, "Bar A", SEXTA, List.of(), List.of());
        umaLata.getCervejas().add(new CervejaDoRole(cerveja(1), FormatoCerveja.LATA, 1));
        CheckIn seisLatoes = checkIn(usuario(2, "Bia"), "Bar A", SEXTA, List.of(), List.of());
        seisLatoes.getCervejas().add(new CervejaDoRole(cerveja(1), FormatoCerveja.LATAO, 6));

        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(List.of(umaLata, seisLatoes));

        assertThat(pontos.get(umaLata.getId()).total()).isEqualTo(pontos.get(seisLatoes.getId()).total());
    }

    @Test
    @DisplayName("Mesmo lugar escrito diferente (acento, caixa, espaco) nao e lugar novo")
    void lugarRepetidoNormalizado() {
        User ana = usuario(1, "Ana");
        CheckIn primeiro = checkIn(ana, "Bar do Zé", SEXTA, List.of(), List.of());
        CheckIn segundo = checkIn(ana, "  bar  do ZE ", SEXTA.plusDays(1), List.of(), List.of());

        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(List.of(primeiro, segundo));

        assertThat(pontos.get(segundo.getId()).lugarNovo()).isFalse();
        assertThat(pontos.get(segundo.getId()).total()).isEqualTo(10);
    }

    @Test
    @DisplayName("Novidade e por pessoa: a cerveja que a amiga ja provou continua nova para mim")
    void novidadePorPessoa() {
        User ana = usuario(1, "Ana");
        User bia = usuario(2, "Bia");
        CheckIn daAna = checkIn(ana, "Bar do Zé", SEXTA, List.of(cerveja(1)), List.of());
        CheckIn daBia = checkIn(bia, "Bar do Zé", SEXTA.plusHours(1), List.of(cerveja(1)), List.of());

        ScoreService.PontosCheckIn pontosBia = scoreService.pontuarCheckIns(List.of(daAna, daBia)).get(daBia.getId());

        assertThat(pontosBia.cervejasNovas()).isEqualTo(1);
        assertThat(pontosBia.lugarNovo()).isTrue();
    }

    @Test
    @DisplayName("Bonus segue a ordem em que o role aconteceu, nao a ordem de registro")
    void ordemCronologica() {
        User ana = usuario(1, "Ana");
        // Registrado primeiro (id menor), mas aconteceu depois
        CheckIn sabado = checkIn(ana, "Bar do Zé", SEXTA.plusDays(1), List.of(cerveja(1)), List.of());
        // Registrado depois (retroativo), mas aconteceu antes
        CheckIn sexta = checkIn(ana, "Bar do Zé", SEXTA, List.of(cerveja(1)), List.of());

        Map<Long, ScoreService.PontosCheckIn> pontos = scoreService.pontuarCheckIns(List.of(sabado, sexta));

        assertThat(pontos.get(sexta.getId()).total()).isEqualTo(20);  // 10 + cerveja nova + lugar novo
        assertThat(pontos.get(sabado.getId()).total()).isEqualTo(10); // nada de novo
    }

    @Test
    @DisplayName("Ranking soma por pessoa, inclui quem nao pontuou e empate divide a posicao")
    void ranking() {
        User ana = usuario(1, "Ana");
        User bia = usuario(2, "Bia");
        User caio = usuario(3, "Caio");
        User duda = usuario(4, "Duda");

        List<CheckIn> checkIns = List.of(
            checkIn(ana, "Bar A", SEXTA, List.of(cerveja(1)), List.of()),                // 20
            checkIn(bia, "Bar A", SEXTA, List.of(cerveja(1)), List.of()),                // 20
            checkIn(caio, "Bar A", SEXTA, List.of(), List.of()),                         // 15
            checkIn(caio, "Bar A", SEXTA.plusDays(1), List.of(), List.of())              // 10 -> 25
        );

        List<ScoreService.PosicaoRanking> ranking = scoreService.ranking(List.of(ana, bia, caio, duda), checkIns);

        assertThat(ranking).extracting(p -> p.usuario().getNome()).containsExactly("Caio", "Ana", "Bia", "Duda");
        assertThat(ranking).extracting(ScoreService.PosicaoRanking::pontos).containsExactly(25, 20, 20, 0);
        assertThat(ranking).extracting(ScoreService.PosicaoRanking::posicao).containsExactly(1, 2, 2, 4);
        assertThat(ranking.get(0).checkIns()).isEqualTo(2);
        assertThat(ranking.get(0).lugaresNovos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sem nenhum check-in, todo mundo empata em primeiro com zero")
    void rankingVazio() {
        List<ScoreService.PosicaoRanking> ranking = scoreService.ranking(
            List.of(usuario(1, "Ana"), usuario(2, "Bia")), List.of()
        );

        assertThat(ranking).extracting(ScoreService.PosicaoRanking::posicao).containsExactly(1, 1);
        assertThat(ranking).extracting(ScoreService.PosicaoRanking::pontos).containsOnly(0);
    }

    @Test
    @DisplayName("Ajuste do admin soma no total, reordena o ranking e vem com o motivo")
    void ajustesDoAdmin() {
        User ana = usuario(1, "Ana");
        User bia = usuario(2, "Bia");
        User caio = usuario(3, "Caio");
        // Ana 15 (check-in + lugar novo), Bia 15
        CheckIn daAna = checkIn(ana, "Bar do Zé", SEXTA, List.of(), List.of());
        CheckIn daBia = checkIn(bia, "Casa da Bia", SEXTA.plusHours(1), List.of(), List.of());

        com.saideira.backend.model.AjustePontos menosDez = new com.saideira.backend.model.AjustePontos();
        menosDez.setUsuario(ana);
        menosDez.setPontos(-10);
        menosDez.setMotivo("check-in repetido");
        // Caio saiu do grupo mas ganhou bonus: aparece mesmo assim
        com.saideira.backend.model.AjustePontos bonus = new com.saideira.backend.model.AjustePontos();
        bonus.setUsuario(caio);
        bonus.setPontos(7);
        bonus.setMotivo("organizou o churrasco");

        List<ScoreService.PosicaoRanking> ranking = scoreService.ranking(
            List.of(ana, bia), List.of(daAna, daBia), List.of(menosDez, bonus));

        assertThat(ranking).extracting(p -> p.usuario().getNome()).containsExactly("Bia", "Caio", "Ana");
        assertThat(ranking).extracting(ScoreService.PosicaoRanking::pontos).containsExactly(15, 7, 5);
        ScoreService.PosicaoRanking linhaAna = ranking.get(2);
        assertThat(linhaAna.ajuste()).isEqualTo(-10);
        assertThat(linhaAna.checkIns()).isEqualTo(1);
        assertThat(linhaAna.ajustes()).extracting(a -> a.getMotivo()).containsExactly("check-in repetido");
        assertThat(ranking.get(0).ajuste()).isZero();
        assertThat(ranking.get(0).ajustes()).isEmpty();
    }
}

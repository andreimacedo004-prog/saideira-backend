package com.saideira.backend.service;

import com.saideira.backend.model.Beer;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O placar do Saideira. Calculo puro: recebe check-ins, devolve pontos.
 * Nao toca no banco, nao tem estado — por isso e o service mais testado.
 *
 * Regras (sempre dentro de UM desafio):
 *   +10 por check-in
 *   +5  por cerveja que a pessoa ainda nao tinha registrado no desafio
 *   +3  por amigo marcado
 *   +5  por lugar onde a pessoa ainda nao tinha feito check-in no desafio
 *
 * "Ainda nao tinha" segue a ordem em que os roles ACONTECERAM (feitoEm),
 * nao a ordem de registro. Quem registra o role de ontem hoje nao perde
 * nem rouba bonus por causa disso.
 *
 * Pontuamos role, variedade e galera — nunca quantidade de bebida.
 */
@Service
public class ScoreService {

    public static final int PONTOS_POR_CHECKIN = 10;
    public static final int PONTOS_POR_CERVEJA_NOVA = 5;
    public static final int PONTOS_POR_AMIGO_MARCADO = 3;
    public static final int PONTOS_POR_LUGAR_NOVO = 5;

    /** Quanto um check-in rendeu e de onde vieram os pontos. */
    public record PontosCheckIn(int total, int cervejasNovas, int amigosMarcados, boolean lugarNovo) {}

    /** Uma linha do ranking. */
    public record PosicaoRanking(
        int posicao,
        User usuario,
        int pontos,
        int checkIns,
        int cervejasNovas,
        int amigosMarcados,
        int lugaresNovos
    ) {}

    /** Ordem cronologica do role; empate de horario desempata pelo id (ordem de registro). */
    private static final Comparator<CheckIn> CRONOLOGICA = Comparator
        .comparing(CheckIn::getFeitoEm, Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
        .thenComparing(CheckIn::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    /**
     * Pontua cada check-in de um desafio.
     * Precisa receber TODOS os check-ins do desafio: o bonus de "novo"
     * depende do que a pessoa ja tinha feito antes.
     */
    public Map<Long, PontosCheckIn> pontuarCheckIns(Collection<CheckIn> checkInsDoDesafio) {
        List<CheckIn> emOrdem = new ArrayList<>(checkInsDoDesafio);
        emOrdem.sort(CRONOLOGICA);

        Map<Long, Set<Long>> cervejasJaRegistradas = new HashMap<>();
        Map<Long, Set<String>> lugaresJaVisitados = new HashMap<>();
        Map<Long, PontosCheckIn> resultado = new LinkedHashMap<>();

        for (CheckIn checkIn : emOrdem) {
            Long autorId = checkIn.getAutor().getId();

            Set<Long> cervejasDoAutor = cervejasJaRegistradas.computeIfAbsent(autorId, id -> new HashSet<>());
            int cervejasNovas = 0;
            for (Beer cerveja : checkIn.getCervejas()) {
                if (cervejasDoAutor.add(cerveja.getId())) {
                    cervejasNovas++;
                }
            }

            boolean lugarNovo = lugaresJaVisitados
                .computeIfAbsent(autorId, id -> new HashSet<>())
                .add(checkIn.getLocalNormalizado());

            int amigos = checkIn.getAmigosMarcados().size();

            int total = PONTOS_POR_CHECKIN
                + cervejasNovas * PONTOS_POR_CERVEJA_NOVA
                + amigos * PONTOS_POR_AMIGO_MARCADO
                + (lugarNovo ? PONTOS_POR_LUGAR_NOVO : 0);

            resultado.put(checkIn.getId(), new PontosCheckIn(total, cervejasNovas, amigos, lugarNovo));
        }
        return resultado;
    }

    /**
     * Monta o ranking de um desafio.
     *
     * @param participantes quem aparece no ranking mesmo sem pontuar (os membros do grupo)
     * @param checkInsDoDesafio todos os check-ins do desafio
     *
     * Ordena por pontos; empate em pontos divide a posicao (1o, 1o, 3o) e, so
     * para a ordem da lista, desempata por quem fez mais check-ins e depois pelo nome.
     */
    public List<PosicaoRanking> ranking(Collection<User> participantes, Collection<CheckIn> checkInsDoDesafio) {
        Map<Long, PontosCheckIn> pontosPorCheckIn = pontuarCheckIns(checkInsDoDesafio);

        Map<Long, User> usuarios = new LinkedHashMap<>();
        participantes.forEach(u -> usuarios.put(u.getId(), u));
        checkInsDoDesafio.forEach(c -> usuarios.putIfAbsent(c.getAutor().getId(), c.getAutor()));

        Map<Long, int[]> placar = new HashMap<>(); // [pontos, checkIns, cervejasNovas, amigos, lugaresNovos]
        usuarios.keySet().forEach(id -> placar.put(id, new int[5]));

        for (CheckIn checkIn : checkInsDoDesafio) {
            PontosCheckIn p = pontosPorCheckIn.get(checkIn.getId());
            int[] linha = placar.get(checkIn.getAutor().getId());
            linha[0] += p.total();
            linha[1] += 1;
            linha[2] += p.cervejasNovas();
            linha[3] += p.amigosMarcados();
            linha[4] += p.lugarNovo() ? 1 : 0;
        }

        List<User> ordenados = new ArrayList<>(usuarios.values());
        ordenados.sort(Comparator
            .comparingInt((User u) -> -placar.get(u.getId())[0])
            .thenComparingInt(u -> -placar.get(u.getId())[1])
            .thenComparing(User::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<PosicaoRanking> ranking = new ArrayList<>();
        int posicao = 0;
        Integer pontosAnterior = null;
        for (int i = 0; i < ordenados.size(); i++) {
            User u = ordenados.get(i);
            int[] linha = placar.get(u.getId());
            if (pontosAnterior == null || linha[0] != pontosAnterior) {
                posicao = i + 1;
                pontosAnterior = linha[0];
            }
            ranking.add(new PosicaoRanking(posicao, u, linha[0], linha[1], linha[2], linha[3], linha[4]));
        }
        return ranking;
    }
}

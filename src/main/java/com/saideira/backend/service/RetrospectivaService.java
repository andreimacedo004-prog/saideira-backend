package com.saideira.backend.service;

import com.saideira.backend.dto.CervejaResponse;
import com.saideira.backend.dto.RetrospectivaResponse;
import com.saideira.backend.dto.RetrospectivaResponse.CervejaConsumida;
import com.saideira.backend.dto.RetrospectivaResponse.Consumo;
import com.saideira.backend.model.Beer;
import com.saideira.backend.model.CervejaDoRole;
import com.saideira.backend.model.Challenge;
import com.saideira.backend.model.CheckIn;
import com.saideira.backend.repository.CheckInRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Soma o que foi registrado de formato x quantidade nos check-ins.
 * E a "soma escondida": nao aparece no feed nem no ranking.
 */
@Service
public class RetrospectivaService {

    private final ChallengeService challengeService;
    private final CheckInRepository checkInRepository;

    public RetrospectivaService(ChallengeService challengeService, CheckInRepository checkInRepository) {
        this.challengeService = challengeService;
        this.checkInRepository = checkInRepository;
    }

    @Transactional(readOnly = true)
    public RetrospectivaResponse doDesafio(Long desafioId, Long usuarioId) {
        Challenge desafio = challengeService.buscarDoMembro(desafioId, usuarioId);
        return calcular(desafio.getId(), checkInRepository.findDoDesafio(desafio.getId()), usuarioId);
    }

    /** Calculo puro, separado para ser testado sem banco. */
    static RetrospectivaResponse calcular(Long desafioId, List<CheckIn> checkIns, Long usuarioId) {
        List<CheckIn> meus = checkIns.stream().filter(c -> c.getAutor().getId().equals(usuarioId)).toList();

        // Minhas cervejas, da que eu mais tomei (em volume) para a que menos tomei
        Map<Long, int[]> porCerveja = new LinkedHashMap<>(); // cervejaId -> [unidades, ml]
        Map<Long, Beer> cervejas = new LinkedHashMap<>();
        for (CheckIn c : meus) {
            for (CervejaDoRole item : c.getCervejas()) {
                Long id = item.getCerveja().getId();
                cervejas.putIfAbsent(id, item.getCerveja());
                int[] soma = porCerveja.computeIfAbsent(id, k -> new int[2]);
                soma[0] += item.getQuantidade();
                soma[1] += item.mililitros();
            }
        }
        List<CervejaConsumida> minhasCervejas = porCerveja.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<Long, int[]> e) -> -e.getValue()[1]))
            .map(e -> new CervejaConsumida(
                CervejaResponse.de(cervejas.get(e.getKey())), e.getValue()[0], litros(e.getValue()[1])))
            .toList();

        return new RetrospectivaResponse(desafioId, somar(meus), somar(checkIns), minhasCervejas);
    }

    private static Consumo somar(List<CheckIn> checkIns) {
        int unidades = 0;
        int mililitros = 0;
        Set<Long> diferentes = new HashSet<>();
        for (CheckIn c : checkIns) {
            for (CervejaDoRole item : c.getCervejas()) {
                unidades += item.getQuantidade();
                mililitros += item.mililitros();
                diferentes.add(item.getCerveja().getId());
            }
        }
        return new Consumo(checkIns.size(), unidades, litros(mililitros), diferentes.size());
    }

    /** 12345 ml -> 12.3 L */
    private static double litros(int mililitros) {
        return Math.round(mililitros / 100.0) / 10.0;
    }
}

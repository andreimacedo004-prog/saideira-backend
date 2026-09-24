package com.saideira.backend.dto;

import com.saideira.backend.model.Reaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contagem de um tipo de reacao num check-in, e se quem esta vendo
 * ja reagiu assim (para o botao aparecer marcado).
 */
public record ReacaoResumo(
    Reaction.Tipo tipo,
    long total,
    boolean reagi
) {

    /**
     * Converte as linhas de ReactionRepository.resumoPorCheckIn
     * ([checkInId, tipo, total, reagi]) num mapa checkInId -> reacoes.
     */
    public static Map<Long, List<ReacaoResumo>> agrupar(List<Object[]> linhas) {
        Map<Long, List<ReacaoResumo>> porCheckIn = new HashMap<>();
        for (Object[] linha : linhas) {
            Long checkInId = (Long) linha[0];
            Reaction.Tipo tipo = (Reaction.Tipo) linha[1];
            long total = ((Number) linha[2]).longValue();
            boolean reagi = ((Number) linha[3]).longValue() > 0;
            porCheckIn.computeIfAbsent(checkInId, id -> new ArrayList<>())
                .add(new ReacaoResumo(tipo, total, reagi));
        }
        porCheckIn.values().forEach(lista -> lista.sort(Comparator.comparing(ReacaoResumo::tipo)));
        return porCheckIn;
    }
}

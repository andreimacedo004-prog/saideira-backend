package com.saideira.backend.dto;

import com.saideira.backend.model.CheckIn;
import com.saideira.backend.service.ScoreService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Um card do feed: tudo que a tela precisa para desenhar o check-in. */
public record CheckInResponse(
    Long id,
    Long desafioId,
    UsuarioResumo autor,
    CheckIn.TipoRole tipo,
    String local,
    String fotoUrl,
    String legenda,
    LocalDateTime feitoEm,
    List<UsuarioResumo> amigos,
    List<CervejaResponse> cervejas,
    PontosResponse pontos,
    List<ReacaoResumo> reacoes,
    long totalComentarios
) {
    public static CheckInResponse de(
        CheckIn c, ScoreService.PontosCheckIn pontos, List<ReacaoResumo> reacoes, long totalComentarios
    ) {
        return new CheckInResponse(
            c.getId(),
            c.getDesafio().getId(),
            UsuarioResumo.de(c.getAutor()),
            c.getTipo(),
            c.getLocal(),
            c.getFotoUrl(),
            c.getLegenda(),
            c.getFeitoEm(),
            c.getAmigosMarcados().stream()
                .map(UsuarioResumo::de)
                .sorted(Comparator.comparing(UsuarioResumo::nome, String.CASE_INSENSITIVE_ORDER))
                .toList(),
            // So o nome da cerveja vai para o feed; formato e quantidade ficam escondidos
            c.getCervejas().stream()
                .map(item -> CervejaResponse.de(item.getCerveja()))
                .sorted(Comparator.comparing(CervejaResponse::nome, String.CASE_INSENSITIVE_ORDER))
                .toList(),
            PontosResponse.de(pontos),
            reacoes,
            totalComentarios
        );
    }
}

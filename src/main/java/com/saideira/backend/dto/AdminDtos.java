package com.saideira.backend.dto;

import com.saideira.backend.model.CheckIn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** O que a area de admin recebe e devolve. So contas de admin chegam a ver isto. */
public final class AdminDtos {

    private AdminDtos() {}

    public record Resumo(long usuarios, long grupos, long desafios, long checkIns, long checkInsUltimos7Dias) {}

    public record Usuario(
        Long id, String nome, String email, LocalDateTime criadoEm, List<String> grupos, long checkIns, boolean admin
    ) {}

    /** O que some e o que muda de dono se a conta for excluida. `bloqueio` != null: nao da para excluir. */
    public record PreviaExclusao(
        UsuarioResumo usuario,
        long checkIns,
        long comentarios,
        long reacoes,
        List<Repasse> gruposRepassados,
        List<String> gruposApagados,
        String bloqueio
    ) {}

    public record Repasse(String grupo, String novoDono) {}

    public record SenhaTemporaria(String senha) {}

    public record Desafio(
        Long id, String nome, String grupoNome, DesafioResponse.Status status,
        LocalDate dataInicio, LocalDate dataFim, int membros, long checkIns,
        Integer intervaloMinimoMinutos
    ) {}

    /** Minutos entre check-ins so neste desafio. Nulo volta ao padrao do app. */
    public record NovoIntervalo(
        @Min(value = 15, message = "O intervalo mínimo é de 15 minutos")
        @Max(value = 1440, message = "O intervalo máximo é de 24 horas")
        Integer minutos
    ) {}

    public record DesafioDetalhe(
        DesafioResponse desafio,
        List<RankingItemResponse> ranking,
        List<CheckInLinha> checkIns,
        List<Ajuste> ajustes,
        List<UsuarioResumo> participantes
    ) {}

    public record CheckInLinha(
        Long id, UsuarioResumo autor, String local, CheckIn.TipoRole tipo, LocalDateTime feitoEm, int pontos, long comentarios
    ) {}

    public record Ajuste(Long id, UsuarioResumo usuario, int pontos, String motivo, LocalDateTime criadoEm, String criadoPor) {}

    public record NovoAjuste(
        @NotNull(message = "Escolha a pessoa") Long usuarioId,
        @NotNull(message = "Informe os pontos")
        @Min(value = -1000, message = "No máximo 1000 pontos por ajuste")
        @Max(value = 1000, message = "No máximo 1000 pontos por ajuste")
        Integer pontos,
        @NotBlank(message = "Escreva o motivo")
        @Size(min = 3, max = 200, message = "O motivo precisa ter de 3 a 200 caracteres")
        String motivo
    ) {}
}

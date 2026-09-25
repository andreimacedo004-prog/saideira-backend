package com.saideira.backend.dto;

import com.saideira.backend.model.CheckIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O que a tela de check-in envia.
 *
 * - feitoEm: opcional. Sem ele, vale o horario do servidor. Serve para quem
 *   esqueceu de registrar na hora (aceita ate 24h atras).
 * - fotoUrl: o PWA sobe a foto direto no Cloudinary e manda so o link.
 * - cervejas: no maximo 5 por check-in — sem limite, daria para farmar
 *   o bonus de cerveja nova cadastrando 30 cervejas num role so.
 *   Formato e quantidade de cada uma nao valem ponto: vao so para a retrospectiva.
 */
public record RegistrarCheckInRequest(
    @NotNull CheckIn.TipoRole tipo,
    @NotBlank @Size(max = 120) String local,
    @Size(max = 1024) @Pattern(regexp = "^https?://.+", message = "A foto precisa ser um link http(s)")
    String fotoUrl,
    @Size(max = 500) String legenda,
    LocalDateTime feitoEm,
    @Size(max = 30) List<Long> amigosIds,
    @Size(max = RegistrarCheckInRequest.MAX_CERVEJAS, message = "No máximo 5 cervejas por check-in")
    List<@Valid ItemCervejaRequest> cervejas
) {
    public static final int MAX_CERVEJAS = 5;
}

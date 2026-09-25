package com.saideira.backend.dto;

import com.saideira.backend.model.CheckIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O que a tela de editar check-in envia. Mesmos campos e limites do
 * registro, menos o horario: ele nao muda, senao daria para burlar a
 * regra do intervalo minimo entre check-ins.
 *
 * Os campos substituem os atuais: fotoUrl vazia ou ausente tira a foto,
 * lista de amigos/cervejas vazia (ou ausente) tira todos.
 */
public record EditarCheckInRequest(
    @NotNull CheckIn.TipoRole tipo,
    @NotBlank @Size(max = 120) String local,
    // Vazio ou ausente = sem foto
    @Size(max = 1024) @Pattern(regexp = "^(https?://.+)?$", message = "A foto precisa ser um link http(s)")
    String fotoUrl,
    @Size(max = 500) String legenda,
    @Size(max = 30) List<Long> amigosIds,
    @Size(max = RegistrarCheckInRequest.MAX_CERVEJAS, message = "No máximo 5 cervejas por check-in")
    List<@Valid ItemCervejaRequest> cervejas
) {}

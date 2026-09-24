package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Usado quando alguem abre o link de convite e entra no grupo. */
public record EntrarNoGrupoRequest(
    @NotBlank String codigoConvite
) {}

package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarGrupoRequest(
    @NotBlank @Size(max = 60) String nome
) {}

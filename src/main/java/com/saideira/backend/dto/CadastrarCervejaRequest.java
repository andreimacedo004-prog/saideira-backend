package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarCervejaRequest(
    @NotBlank @Size(max = 100) String nome,
    @Size(max = 60) String estilo,      // opcional
    @Size(max = 80) String cervejaria   // opcional
) {}

package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(
    @NotBlank @Size(max = 60) String nome,
    @Size(max = 255) String bio, // opcional
    @Size(max = 1024) @Pattern(regexp = "^https?://.+", message = "A foto precisa ser um link http(s)")
    String fotoUrl               // opcional
) {}

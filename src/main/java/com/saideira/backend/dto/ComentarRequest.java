package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarRequest(
    @NotBlank @Size(max = 500) String texto
) {}

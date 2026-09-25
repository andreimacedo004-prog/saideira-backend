package com.saideira.backend.dto;

import com.saideira.backend.model.FormatoCerveja;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemCervejaRequest(
    @NotNull Long cervejaId,
    @NotNull FormatoCerveja formato,
    @Min(value = 1, message = "A quantidade mínima é 1")
    @Max(value = 20, message = "A quantidade máxima é 20 por cerveja")
    int quantidade
) {}

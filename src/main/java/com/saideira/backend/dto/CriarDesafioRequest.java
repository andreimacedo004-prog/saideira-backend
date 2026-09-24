package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Datas no formato "2026-11-01". As duas sao inclusivas. */
public record CriarDesafioRequest(
    @NotBlank @Size(max = 80) String nome,
    @NotNull LocalDate dataInicio,
    @NotNull LocalDate dataFim
) {}

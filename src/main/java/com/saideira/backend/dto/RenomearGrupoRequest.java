package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenomearGrupoRequest(
    @NotBlank(message = "Dê um nome para o grupo") @Size(max = 60, message = "O nome pode ter até 60 caracteres") String nome
) {}

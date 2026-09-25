package com.saideira.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** So o nome muda: as datas ficam, para nao bagunçar check-ins ja feitos. */
public record RenomearDesafioRequest(
    @NotBlank(message = "Dê um nome para o desafio") @Size(max = 80, message = "O nome pode ter até 80 caracteres") String nome
) {}

package com.saideira.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados da tela de cadastro.
 * DTOs existem para nao expor a entidade User na API
 * (ex: nunca aceitar 'senhaHash' vindo do cliente).
 */
public record CadastroRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres") String senha,
    @NotBlank @Size(max = 60) String nome,

    // App de role com cerveja: a pessoa confirma que tem 18+ no cadastro
    @NotNull @AssertTrue(message = "O Saideira é só para maiores de 18 anos") Boolean maiorDeIdade
) {}

package com.ourosapp.springapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para operações de login.
 *
 * @param email    o e-mail do usuário
 * @param password a senha em texto plano
 */
public record LoginRequestDTO(

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    String password
) {}

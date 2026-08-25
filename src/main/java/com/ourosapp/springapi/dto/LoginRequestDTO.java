package com.ourosapp.springapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requisição para operações de login.
 *
 * @param email    o e-mail do usuário
 * @param password a senha em texto plano
 */
@Schema(description = "Dados de autenticação de usuário")
public record LoginRequestDTO(

    @Schema(description = "E-mail de acesso do usuário", example = "usuario@ourosapp.com")
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    String email,

    @Schema(description = "Senha de acesso do usuário", example = "123456")
    @NotBlank(message = "A senha é obrigatória.")
    String password
) {}

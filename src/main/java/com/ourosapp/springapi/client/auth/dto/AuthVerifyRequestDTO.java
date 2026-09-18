package com.ourosapp.springapi.client.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload enviado ao ms-auth-service no endpoint POST /v1/auth/credentials/verify.
 *
 * @param email       e-mail do usuário
 * @param password    senha em texto simples
 * @param accountType tipo de conta opcional ("admin", "company_employee", "farm_owner")
 */
public record AuthVerifyRequestDTO(
        @JsonProperty("email") String email,
        @JsonProperty("password") String password,
        @JsonProperty("account_type") String accountType
) {
    public AuthVerifyRequestDTO {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}

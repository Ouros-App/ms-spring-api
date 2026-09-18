package com.ourosapp.springapi.client.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta retornada pelo ms-auth-service na verificação de credenciais.
 *
 * @param authenticated indica se as credenciais foram validadas com sucesso
 * @param identity      identidade do usuário autenticado
 */
public record AuthVerifyResponseDTO(
        @JsonProperty("authenticated") boolean authenticated,
        @JsonProperty("identity") IdentityDTO identity
) {
    /**
     * Dados da identidade autenticada correspondentes ao registro no banco de dados.
     *
     * @param id           identificador do usuário no PostgreSQL
     * @param email        e-mail normalizado
     * @param accountType  tipo de conta ("admin", "company_employee", "farm_owner")
     * @param realmRole    role correspondente no Keycloak
     * @param name         nome do usuário (se aplicável)
     * @param farmId       ID da fazenda vinculada (para farm_owner)
     * @param enterpriseId ID da empresa vinculada (para company_employee)
     * @param firstAccess  indicador de primeiro acesso (para farm_owner)
     */
    public record IdentityDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("email") String email,
            @JsonProperty("account_type") String accountType,
            @JsonProperty("realm_role") String realmRole,
            @JsonProperty("name") String name,
            @JsonProperty("farm_id") Long farmId,
            @JsonProperty("enterprise_id") Long enterpriseId,
            @JsonProperty("first_access") Boolean firstAccess
    ) {}
}

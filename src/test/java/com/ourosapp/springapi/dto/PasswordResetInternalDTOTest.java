package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.internal.PasswordResetInternalDTO;
import com.ourosapp.springapi.dto.internal.PasswordResetInternalResponseDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetInternalDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve validar com sucesso quando todos os campos forem válidos")
    void shouldValidateSuccessfully() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO(
                "farm_owner",
                10L,
                "NovaSenha@123"
        );

        assertEquals("farm_owner", dto.accountType());
        assertEquals(10L, dto.id());
        assertEquals("NovaSenha@123", dto.newPassword());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("Deve normalizar espaços e letras maiúsculas em accountType e newPassword")
    void shouldSanitizeFields() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO(
                "  FARM_OWNER  ",
                10L,
                "  NovaSenha@123  "
        );

        assertEquals("farm_owner", dto.accountType());
        assertEquals(10L, dto.id());
        assertEquals("NovaSenha@123", dto.newPassword());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("Deve desserializar tanto camelCase quanto snake_case")
    void shouldDeserializeCamelCaseAndSnakeCase() throws Exception {
        String jsonSnake = """
                {
                    "account_type": "company_employee",
                    "id": 5,
                    "new_password": "SenhaValida@2026"
                }
                """;
        PasswordResetInternalDTO fromSnake = objectMapper.readValue(jsonSnake, PasswordResetInternalDTO.class);
        assertEquals("company_employee", fromSnake.accountType());
        assertEquals(5L, fromSnake.id());
        assertEquals("SenhaValida@2026", fromSnake.newPassword());
        assertTrue(validator.validate(fromSnake).isEmpty());

        String jsonCamel = """
                {
                    "accountType": "farm_owner",
                    "id": 8,
                    "newPassword": "OutraSenha@999"
                }
                """;
        PasswordResetInternalDTO fromCamel = objectMapper.readValue(jsonCamel, PasswordResetInternalDTO.class);
        assertEquals("farm_owner", fromCamel.accountType());
        assertEquals(8L, fromCamel.id());
        assertEquals("OutraSenha@999", fromCamel.newPassword());
        assertTrue(validator.validate(fromCamel).isEmpty());
    }

    @Test
    @DisplayName("Deve falhar validação quando campos forem inválidos")
    void shouldFailValidationOnInvalidFields() {
        PasswordResetInternalDTO invalidDto = new PasswordResetInternalDTO(
                "invalid_role",
                -1L,
                "fraca"
        );
        var violations = validator.validate(invalidDto);
        assertEquals(3, violations.size());
    }

    @Test
    @DisplayName("Deve instanciar response DTO corretamente")
    void shouldInstantiateResponseDto() {
        PasswordResetInternalResponseDTO response = new PasswordResetInternalResponseDTO("Sucesso");
        assertEquals("Sucesso", response.message());
    }
}

package com.ourosapp.springapi.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CnpjValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678000195",
            "11222333000181",
            "04252011000110",
            "98765432000198"
    })
    @DisplayName("Deve retornar true para CNPJs válidos")
    void testValidCnpjs(String cnpj) {
        assertTrue(CnpjValidator.isValid(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000000",
            "11111111111111",
            "22222222222222",
            "33333333333333",
            "44444444444444",
            "55555555555555",
            "66666666666666",
            "77777777777777",
            "88888888888888",
            "99999999999999"
    })
    @DisplayName("Deve rejeitar sequências com todos os dígitos iguais")
    void testRepeatedDigits(String cnpj) {
        assertFalse(CnpjValidator.isValid(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678000105", // Primeiro dígito verificador incorreto (esperado 9, fornecido 0)
            "12345678000190", // Segundo dígito verificador incorreto (esperado 5, fornecido 0)
            "12345678000100", // Ambos os dígitos verificadores incorretos
            "11222333000100"  // Dígitos verificadores incorretos para a base
    })
    @DisplayName("Deve rejeitar CNPJs com dígitos verificadores inválidos")
    void testInvalidCheckDigits(String cnpj) {
        assertFalse(CnpjValidator.isValid(cnpj));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "1234567890123",    // 13 dígitos
            "123456789012345",  // 15 dígitos
            "1234567800019A",   // Contém letra
            "12.345.678/0001-95"// Formatado com pontuação (a validação espera dígitos puros da DTO)
    })
    @DisplayName("Deve rejeitar valores nulos, vazios, com formato ou tamanho incorreto")
    void testInvalidFormats(String cnpj) {
        assertFalse(CnpjValidator.isValid(cnpj));
    }
}

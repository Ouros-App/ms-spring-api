package com.ourosapp.springapi.util;

/**
 * Utilitário responsável pela validação algorítmica de Cadastro Nacional da Pessoa Jurídica (CNPJ).
 * Implementa as regras oficiais da Receita Federal do Brasil (cálculo módulo 11).
 */
public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST_DIGIT = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
        // Construtor privado para evitar instanciação de classe utilitária
    }

    /**
     * Valida se uma cadeia de caracteres representa um CNPJ matematicamente válido.
     *
     * @param cnpj string contendo os 14 dígitos numéricos do CNPJ (apenas números)
     * @return {@code true} se o CNPJ for válido, {@code false} caso contrário
     */
    public static boolean isValid(String cnpj) {
        if (cnpj == null || cnpj.length() != 14 || !cnpj.chars().allMatch(Character::isDigit)) {
            return false;
        }

        // Rejeita sequências repetidas de 14 dígitos (ex: 00000000000000, 11111111111111, etc.)
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(cnpj, WEIGHTS_FIRST_DIGIT, 12);
        if (firstDigit != Character.getNumericValue(cnpj.charAt(12))) {
            return false;
        }

        int secondDigit = calculateDigit(cnpj, WEIGHTS_SECOND_DIGIT, 13);
        return secondDigit == Character.getNumericValue(cnpj.charAt(13));
    }

    /**
     * Calcula o dígito verificador com base nos pesos e tamanho especificados.
     *
     * @param cnpj    cadeia de caracteres do CNPJ
     * @param weights array de pesos ponderados
     * @param length  quantidade de dígitos a considerar no cálculo
     * @return o dígito verificador calculado (0 a 9)
     */
    private static int calculateDigit(String cnpj, int[] weights, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}

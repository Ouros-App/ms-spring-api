package com.ourosapp.springapi.constants;

/**
 * Constantes centralizadas para mensagens de erro reutilizadas em múltiplos serviços da aplicação.
 */
public final class ErrorMessages {

    public static final String EMPLOYEE_NOT_FOUND = "Funcionário não encontrado";
    public static final String USER_NOT_AUTHENTICATED = "Usuário não autenticado";

    private ErrorMessages() {
        // Classe utilitária — não deve ser instanciada
    }
}

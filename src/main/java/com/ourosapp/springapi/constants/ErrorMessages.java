package com.ourosapp.springapi.constants;

/**
 * Constantes centralizadas para mensagens de erro reutilizadas em múltiplos serviços da aplicação.
 */
public final class ErrorMessages {

    public static final String EMPLOYEE_NOT_FOUND = "Funcionário não encontrado";
    public static final String USER_NOT_AUTHENTICATED = "Usuário não autenticado";
    public static final String FARM_OWNER_NOT_FOUND = "Produtor rural não encontrado";
    public static final String ACCESS_DENIED_ADDRESS = "Acesso negado a este endereço";

    private ErrorMessages() {
        // Classe utilitária — não deve ser instanciada
    }
}

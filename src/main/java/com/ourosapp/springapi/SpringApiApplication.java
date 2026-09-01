package com.ourosapp.springapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação Spring Boot que inicializa e configura o contexto da API REST.
 * Ponto de entrada da aplicação Ouros App MS Spring API.
 */
@SpringBootApplication
public class SpringApiApplication {

    /**
     * Método principal que inicializa a aplicação Spring Boot.
     *
     * @param args argumentos da linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringApiApplication.class, args);
    }
}

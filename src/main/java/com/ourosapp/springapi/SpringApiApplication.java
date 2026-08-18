package com.ourosapp.springapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

// Desativa a seguranca padrao temporariamente ate a implementacao do modulo JWT na proxima PR
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class SpringApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringApiApplication.class, args);
    }
}

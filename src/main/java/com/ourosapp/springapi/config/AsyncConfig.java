package com.ourosapp.springapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de execução assíncrona para processamento desacoplado de eventos e notificações.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool de threads dedicado para despacho assíncrono de notificações push e eventos de ranking.
     *
     * @return Executor configurado para tarefas de notificação
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-task-");
        executor.initialize();
        return executor;
    }
}

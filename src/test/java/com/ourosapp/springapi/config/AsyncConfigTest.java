package com.ourosapp.springapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    @DisplayName("notificationTaskExecutor - Deve configurar e inicializar ThreadPoolTaskExecutor")
    void deveConfigurarExecutorDeNotificacoes() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.notificationTaskExecutor();

        assertNotNull(executor);
        assertTrue(executor instanceof ThreadPoolTaskExecutor);

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(4, taskExecutor.getCorePoolSize());
        assertEquals(16, taskExecutor.getMaxPoolSize());
        assertEquals(500, taskExecutor.getQueueCapacity());
        assertEquals("notification-task-", taskExecutor.getThreadNamePrefix());

        taskExecutor.shutdown();
    }
}

package edu.upb.eventop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita la ejecución asíncrona (@Async) y define el pool de hilos dedicado
 * a la escritura de logs. Usar un executor propio (en vez del SimpleAsyncTaskExecutor
 * por defecto) evita crear un hilo nuevo por cada log y permite controlar la cola.
 *
 * setWaitForTasksToCompleteOnShutdown(true) hace que, ante un apagado ordenado,
 * se vacíe la cola de logs pendientes antes de cerrar (mitiga la pérdida de logs).
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("log-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

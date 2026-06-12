package edu.upb.eventop.config;

import edu.upb.eventop.repository.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class InjectConfiguration {

    // Leemos los parametros del pool desde application.properties y los
    // guardamos en variables de esta clase para usarlos al construir el pool.
    @Value("${async.core-pool-size:5}")
    private int corePoolSize;
    @Value("${async.max-pool-size:5}")
    private int maxPoolSize;
    @Value("${async.queue-capacity:10}")
    private int queueCapacity;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("ADMIN");
            }
            if(authentication.getPrincipal()==null || authentication.getPrincipal() instanceof String) {
                return Optional.of("ADMIN");
            }

            User user = (User) authentication.getPrincipal();
            try {
                return Optional.of(user.getUsername());
            } catch (Exception e) {
                return Optional.of("ADMIN");
            }
        };
    }

    // Pool de hilos para el logging asincrono (@Async("taskLog")).
    @Bean(name = "taskLog")
    public ThreadPoolTaskExecutor myTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // Número de hilos que siempre estarán activos
        executor.setMaxPoolSize(maxPoolSize); // Número máximo de hilos
        executor.setQueueCapacity(queueCapacity); // Capacidad de la cola para tareas en espera
        executor.setThreadNamePrefix("TaskLog-");
        executor.initialize();
        return executor;
    }

    // Spring detecta este bean (de tipo TaskScheduler) y lo usa automaticamente
    // para ejecutar todos los metodos @Scheduled. Asi el hilo se llama "Miguel-X"
    // en vez del "scheduling-X" por defecto.
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("Miguel-");
        scheduler.initialize();
        return scheduler;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void listarEmpresas(){
        log.info("INFO: " + "Listando Empresas");
    }
}

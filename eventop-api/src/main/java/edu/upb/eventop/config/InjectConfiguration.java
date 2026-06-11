package edu.upb.eventop.config;

import edu.upb.eventop.repository.entity.User;
import edu.upb.eventop.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class InjectConfiguration {
    @Value("${async.core-pool-size:5}")
    private int corePoolSize ;
    @Value("${async.max-pool-size:5}")
    private int maxPoolSize;
    @Value("${async.queue-capacity:10}")
    private int queueCapacity;
    private final LogService logService;

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

    @Bean(name = "taskLog")
    public ThreadPoolTaskExecutor myTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // Número de hilos que siempre estarán activos
        executor.setMaxPoolSize(maxPoolSize); // Número máximo de hilos
        executor.setQueueCapacity(queueCapacity); // Capacidad de la cola para tareas en espera
        executor.setThreadNamePrefix("Miguel-");
        executor.initialize();
        return executor;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void listarEmpresas(){
        log.info("INFO: " + "Listando todos los empresas");
        logService.deleteAll();
    }
}

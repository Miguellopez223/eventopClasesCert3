package edu.upb.eventop;

import edu.upb.eventop.integracion.Sistema1AuthRequest;
import edu.upb.eventop.integracion.Sistema1AuthResponse;
import edu.upb.eventop.integracion.SistemaA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableAsync
@EnableScheduling
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableJpaAuditing
@EnableCaching
@SpringBootApplication
public class EventopApplication implements CommandLineRunner {
	@Autowired
	private SistemaA sistemaA;

	public static void main(String[] args) {
		SpringApplication.run(EventopApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			Sistema1AuthRequest request = new Sistema1AuthRequest();
			request.setUsername("root");
			request.setPassword("Abc123**");
			Sistema1AuthResponse response = sistemaA.auth(request);
			System.out.println(response);
		} catch (Exception e) {
			log.warn("No se pudo autenticar contra Sistema 1 ({}). La aplicacion continua sin esa integracion.", e.getMessage());
		}
	}

}

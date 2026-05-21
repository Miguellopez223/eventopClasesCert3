package edu.upb.eventop;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableJpaAuditing
@SpringBootApplication
public class EventopApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(EventopApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
	}

}

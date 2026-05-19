package edu.upb.eventop;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventopApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(EventopApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
	}

}

package edu.upb.eventop;

import edu.upb.eventop.repository.EmpresaRepository;
import edu.upb.eventop.repository.entity.Empresa;
import edu.upb.eventop.repository.entity.Eventos;
import edu.upb.eventop.repository.EventosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@SpringBootApplication
public class EventopApplication implements CommandLineRunner {

	@Autowired
	private EventosRepository eventosRepository;
	@Autowired
	private EmpresaRepository empresaRepository;

	public static void main(String[] args) {
		SpringApplication.run(EventopApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		utilitario();
	}

	private void utilitario() {

		if(this.empresaRepository.count() == 0) {
			Empresa empresa = new Empresa();
			empresa.setNombre("Empresa 1");
			empresa.setDescripcion("Empresa 1");
			empresaRepository.save(empresa);
		}

		Empresa empresa = this.empresaRepository.findAll().get(0);

		Eventos eventos = new Eventos();
		eventos.setNombre("Eventos de prueba");
		eventos.setDescripcion("Eventos de prueba");
		eventos.setEmpresa(empresa);
		eventosRepository.save(eventos);
	}
}

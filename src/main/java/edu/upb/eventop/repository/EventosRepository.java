package edu.upb.eventop.repository;

import edu.upb.eventop.repository.dto.response.EventoResponseDto;
import edu.upb.eventop.repository.entity.Empresa;
import edu.upb.eventop.repository.entity.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventosRepository extends JpaRepository<Eventos, String> {
    @Query("SELECT e FROM Eventos e INNER JOIN e.empresa ee " +
            "WHERE ee.nombre='Empresa 1' ")
    List<EventoResponseDto> listarEventos();

}

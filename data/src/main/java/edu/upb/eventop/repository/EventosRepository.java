package edu.upb.eventop.repository;

import edu.upb.eventop.repository.dto.response.EventoResponseDto;
import edu.upb.eventop.repository.entity.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventosRepository extends JpaRepository<Eventos, String> {
    @Query("SELECT e FROM Eventos e INNER JOIN FETCH e.materia ee " +
            "WHERE ee.nombreEmpresa='Empresa 1' ")
    List<EventoResponseDto> listarEventos();

    @Query("SELECT e FROM Eventos e INNER JOIN  Empresa ee ON (e.materia = ee) " +
            "WHERE ee.nombreEmpresa='Empresa 1' ")
    List<EventoResponseDto> listarEventos2();

    @Query(value = "SELECT e.id, e.nombre, e.descrpcion " +
            " FROM Eventos e INNER JOIN  empresas ee ON (e.empresa_id = ee.id) " +
            "WHERE ee.nombre='Empresa 1' ", nativeQuery = true)
    List<EventoResponseDto> listarEventos3();


    @Procedure(procedureName = "SP_CREAR_EMPRESA")
    String crearEmpresa(
            @Param("p_nombre") String nombre,
            @Param("p_descripcion") String descripcion
    );
}

package edu.upb.eventop.repository;

import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    @Query("SELECT e FROM Empresa  e WHERE e.nombre='Empresa 1' ")
    List<Empresa> listarEmpresas();

    List<Empresa> findByNombre(String nombre);

    @Query("SELECT e FROM Empresa e WHERE e.nombre=:pNombre")
    List<EmpresaDto> findByNombreAux(@Param("pNombre") String nombre);

    @Query("SELECT new edu.upb.eventop.repository.dto.response.EmpresaDto(e.id, e.nombre)  " +
            "FROM Empresa e WHERE e.nombre=:pNombre")
    List<EmpresaDto> findByNombreAuxB(@Param("pNombre") String nombre);

    @Query("SELECT new edu.upb.eventop.repository.dto.response.EmpresaDto(e.nombre)  " +
            "FROM Empresa e WHERE e.nombre=:pNombre")
    List<EmpresaDto> findByNombreAuxC(@Param("pNombre") String nombre);

}

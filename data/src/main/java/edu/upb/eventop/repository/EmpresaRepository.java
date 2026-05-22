package edu.upb.eventop.repository;

import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    @Query("SELECT e FROM Empresa  e WHERE e.nombreEmpresa='Empresa 1' ")
    List<Empresa> listarEmpresas();

    List<Empresa> findByNombreEmpresa(String nombre);

    @Query("SELECT e FROM Empresa e WHERE e.nombreEmpresa=:pNombre")
    List<EmpresaDto> findByNombreAux(@Param("pNombre") String nombre);

    @Query("SELECT new edu.upb.eventop.repository.dto.response.EmpresaDto(e.id, e.nombreEmpresa)  " +
            "FROM Empresa e WHERE e.nombreEmpresa=:pNombre")
    List<EmpresaDto> findByNombreAuxB(@Param("pNombre") String nombre);

    @Query("SELECT new edu.upb.eventop.repository.dto.response.EmpresaDto(e.nombreEmpresa)  " +
            "FROM Empresa e WHERE e.nombreEmpresa=:pNombre")
    List<EmpresaDto> findByNombreAuxC(@Param("pNombre") String nombre);

    @Modifying
    @Query("UPDATE Empresa e SET e.nombreEmpresa=:pNombre, e.descripcion=:pDescripcion, e.version = e.version+1 WHERE e.id=:pEmpresaId")
    void actualizarEmpresa(
            @Param("pEmpresaId")String pEmpresaId,
            @Param("pNombre")String nombre,
            @Param("pDescripcion")String descripcion);
}

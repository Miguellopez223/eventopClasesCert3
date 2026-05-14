package edu.upb.eventop.repository;

import edu.upb.eventop.repository.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {

}

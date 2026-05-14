package edu.upb.eventop.repository;

import edu.upb.eventop.repository.entity.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventosRepository extends JpaRepository<Eventos, String> {

}

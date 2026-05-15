package edu.upb.eventop.service;

import edu.upb.eventop.repository.EmpresaRepository;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class EmpresaService {
    private final EmpresaRepository repository;

    @Transactional
    public void save(Empresa empresa) {
        this.repository.save(empresa);
    }

    @Transactional(readOnly = true)
    public List<EmpresaDto> listar() {
        return this.repository.findByNombreAux("Empresa 1");
    }

    @Transactional(readOnly = true)
    public Optional<Empresa> findByID(String id) {
        return this.repository.findById(id);
    }

}

package edu.upb.eventop.service;

import edu.upb.eventop.repository.EventosRepository;
import edu.upb.eventop.repository.dto.response.EventoResponseDto;
import edu.upb.eventop.repository.entity.Empresa;
import edu.upb.eventop.repository.entity.Eventos;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class EventosService {
    private final EventosRepository eventosRepository;
    private final EmpresaService empresaService;

    @Transactional(readOnly = true)
    public List<EventoResponseDto> eventos() {
        return eventosRepository.listarEventos3();
    }

    @Transactional
    public void guardar(String empresaId, Eventos eventos) {
        Optional<Empresa> optionalEmpresa = this.empresaService.findByID(empresaId);
        Empresa empresa = null;
        if(optionalEmpresa.isPresent()) {
            empresa = optionalEmpresa.get();
        }
        eventos.setEmpresa(empresa);
        this.eventosRepository.save(eventos);
    }


}

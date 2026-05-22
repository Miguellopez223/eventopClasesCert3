package edu.upb.eventop.service;

import ch.qos.logback.core.util.StringUtil;
import edu.upb.eventop.repository.EmpresaRepository;
import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class EmpresaService {
    private final EmpresaRepository repository;

    @Transactional
    public void save(EmpresaRequestDto empresa) throws Exception {
        if (StringUtil.isNullOrEmpty(empresa.getNombre())) {
            log.error("Error al guardar empresa. El campo nombre null");
            throw new Exception("El campo nombre es null");
        }

        if (StringUtil.isNullOrEmpty(empresa.getDescripcion())) {
            log.error("Error al guardar empresa. El campo Descripcion null");
            throw new Exception("El campo Descripcion es null");
        }

        Empresa empresa1 = new Empresa();
        empresa1.setNombre(empresa.getNombre());
        empresa1.setDescripcion(empresa.getDescripcion());
        this.repository.save(empresa1);
    }


    @Transactional
    public void update(String empresaId, EmpresaRequestDto empresa) throws Exception {
        if (StringUtil.isNullOrEmpty(empresa.getNombre())) {
            log.error("Error al guardar empresa. El campo nombre null");
            throw new Exception("El campo nombre es null");
        }

        if (StringUtil.isNullOrEmpty(empresa.getDescripcion())) {
            log.error("Error al guardar empresa. El campo Descripcion null");
            throw new Exception("El campo Descripcion es null");
        }

        this.repository.actualizarEmpresa(empresaId, empresa.getNombre(), empresa.getDescripcion());

        /*
         Optional<Empresa> optionalEmpresa = this.repository.findById(empresaId);
         if(optionalEmpresa.isEmpty()) {
         throw new Exception("No existe el empresa con el id: " + empresaId);
         }

         Empresa empresa1 = optionalEmpresa.get();
         empresa1.setNombre(empresa.getNombre());
         empresa1.setDescripcion(empresa.getDescripcion());
         //this.repository.save(empresa1);

         */
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

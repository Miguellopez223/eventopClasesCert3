package edu.upb.eventop.service;

import ch.qos.logback.core.util.StringUtil;
import edu.upb.eventop.repository.EmpresaRepository;
import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import edu.upb.eventop.service.exception.NotDataFoundException;
import edu.upb.eventop.service.exception.OperationException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmpresaService {
    private final EmpresaRepository repository;
    private final LogService logService;

    @Autowired
    @Qualifier("ganaderMetodoPago")
    private MetodoPagoService ganaderMetodoPago;

    @Autowired
    @Qualifier("stereumMetodoPago")
    private MetodoPagoService stereumMetodoPago;

    @Transactional(noRollbackFor = NotDataFoundException.class)
    public void save(EmpresaRequestDto empresa) throws Exception {
        logService.infoTx("Iniciando el registro de la empresa: "+empresa.getNombre());
        if (StringUtil.isNullOrEmpty(empresa.getNombre())) {
            log.error("Error al guardar empresa. El campo nombre null");
            logService.errorTx("Error al guardar empresa. El campo nombre null");
            throw new OperationException("El campo nombre es null");
        }

        logService.infoTx("Validando empresa: "+empresa.getNombre());
        if (StringUtil.isNullOrEmpty(empresa.getDescripcion())) {
            log.error("Error al guardar empresa. El campo Descripcion null");
            logService.errorTx("El campo Descripcion es null");
            throw new OperationException("El campo Descripcion es null");
        }

        logService.infoTx("Preparando para registrar:"+ empresa.getNombre());
        Empresa empresa1 = new Empresa();
        empresa1.setNombre(empresa.getNombre());
        empresa1.setDescripcion(empresa.getDescripcion());
        this.repository.save(empresa1);
        //throw new NotDataFoundException("El empresa no existe");

        //logService.infoTx("Registrado en DB. " + empresa.getNombre());
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

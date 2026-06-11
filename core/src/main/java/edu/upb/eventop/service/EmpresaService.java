package edu.upb.eventop.service;

import ch.qos.logback.core.util.StringUtil;
import edu.upb.eventop.repository.EmpresaRepository;
import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.Empresa;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class EmpresaService {
    private final EmpresaRepository repository;
    private final LogService logService;

    // rollbackFor = Exception.class -> por defecto Spring sólo hace rollback ante
    // RuntimeException/Error. Al ser nuestra excepción "checked" (Exception), sin
    // esto NO se revertiría. Con esto, la empresa SÍ hace rollback.
    @Transactional(rollbackFor = Exception.class)
    public void save(EmpresaRequestDto empresa) throws Exception {
        String usuario = getUsuarioActual();
        // Log asíncrono: se dispara y el método sigue de inmediato (no impacta el tiempo).
        this.logService.infoTxAsync("Inicio guardado de empresa: " + empresa.getNombre(), usuario);

        if (StringUtil.isNullOrEmpty(empresa.getNombre())) {
            log.error("Error al guardar empresa. El campo nombre null");
            this.logService.errorTxAsync("Error al guardar empresa. El campo nombre es null", usuario);
            throw new Exception("El campo nombre es null");
        }

        if (StringUtil.isNullOrEmpty(empresa.getDescripcion())) {
            log.error("Error al guardar empresa. El campo Descripcion null");
            this.logService.errorTxAsync("Error al guardar empresa. El campo Descripcion es null", usuario);
            throw new Exception("El campo Descripcion es null");
        }

        Empresa empresa1 = new Empresa();
        empresa1.setNombre(empresa.getNombre());
        empresa1.setDescripcion(empresa.getDescripcion());
        this.repository.save(empresa1);

        this.logService.infoTxAsync("Empresa guardada correctamente: " + empresa.getNombre(), usuario);

        // EXPERIMENTO: forzamos un error DESPUÉS del save para provocar rollback.
        // Resultado esperado:
        //   - La empresa NO se inserta (rollback de la transacción padre).
        //   - El log SÍ queda en la tabla logs (corre en otro hilo, con su propia
        //     transacción REQUIRES_NEW, independiente del rollback del padre).
        throw new Exception("Excepción de prueba para forzar rollback de la empresa");
    }

    /**
     * Resuelve el usuario autenticado. El contexto de seguridad vive en el hilo
     * actual y NO se propaga al hilo async, por eso lo capturamos aquí y lo
     * pasamos como parámetro a LogService.
     */
    private String getUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "ANONIMO";
        }
        return authentication.getName();
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

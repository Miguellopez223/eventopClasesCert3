package edu.upb.eventop.controller;

import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.User;
import edu.upb.eventop.service.EmpresaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Secured({"ROLE_ADMIN", "ROLE_ROOT"})
@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/api/v1/empresas")
public class EmpresaController {
    private final EmpresaService empresaService;

    @GetMapping()
    public ResponseEntity<List<EmpresaDto>> empresas() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("USuario:{} ", user.getUsername());

        try {
            return ResponseEntity.ok(empresaService.listar());
        }catch (Exception e) {
            log.error("Error al listar empresas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> guardar(@RequestBody EmpresaRequestDto empresa) {
        try {
            this.empresaService.save(empresa);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            log.error("Error al guardar empresa", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable("id")String empresaId,
                                        @RequestBody EmpresaRequestDto empresa) {
        try {
            this.empresaService.update(empresaId, empresa);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            log.error("Error al guardar empresa", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

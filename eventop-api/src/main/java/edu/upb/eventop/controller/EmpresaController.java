package edu.upb.eventop.controller;

import edu.upb.eventop.repository.dto.request.EmpresaRequestDto;
import edu.upb.eventop.repository.dto.response.EmpresaDto;
import edu.upb.eventop.repository.entity.User;
import edu.upb.eventop.service.EmpresaService;
import edu.upb.eventop.service.exception.OperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Secured({"ROLE_ADMIN", "ROLE_ROOT"})
@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/api/v1/empresas")
public class EmpresaController {
    private final EmpresaService empresaService;

    @Operation(summary = "Método para listar empresas",
            description = "Este es un API para listar las empresas válidas ",
            tags = {"empresas"},
            responses = {
                    @ApiResponse(description = "Operación satisfactorio", responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EmpresaDto.class))),

                    @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Fallo de autentificación", content = @Content(schema = @Schema(hidden = true))),
            }, security = @SecurityRequirement(name = "bearerToken"))
    @GetMapping()
    public ResponseEntity<List<EmpresaDto>> empresas() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("USuario:{} ", user.getUsername());

        try {
            return ResponseEntity.ok(empresaService.listar());
        } catch (Exception e) {
            log.error("Error al listar empresas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Método para crear una empresa",
            description = "Método para crear una empresa ",
            tags = {"empresas"},
            responses = {
                    @ApiResponse(description = "Operación satisfactorio", responseCode = "200"),
                    @ApiResponse(responseCode = "404", description = "Recurso no encontrado", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Fallo de autentificación", content = @Content(schema = @Schema(hidden = true))),
            }, security = @SecurityRequirement(name = "bearerToken"))
    @PostMapping
    public ResponseEntity<Void> guardar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmpresaRequestDto.class)))
            @RequestBody EmpresaRequestDto empresa) {
        try {
            this.empresaService.save(empresa);
            return ResponseEntity.ok().build();
        } catch (OperationException e) {
            log.error("Error al guardar empresa. Message: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar empresa", e);
            //return ResponseEntity.internalServerError().build();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Se generó un error genérico al guardar empresa");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable("id") String empresaId,
                                           @RequestBody EmpresaRequestDto empresa) {
        try {
            this.empresaService.update(empresaId, empresa);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al guardar empresa", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

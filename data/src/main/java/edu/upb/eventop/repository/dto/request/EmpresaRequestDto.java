package edu.upb.eventop.repository.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaRequestDto {
    @JsonProperty("nombre_empresa")
    private String nombre; //nombre_empresa;
    private String descripcion;
}

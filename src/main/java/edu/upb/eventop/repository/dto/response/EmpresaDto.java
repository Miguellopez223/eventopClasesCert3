package edu.upb.eventop.repository.dto.response;


import edu.upb.eventop.repository.entity.Empresa;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaDto {
    private String id;
    private String nombre;
    private String descripcion;

    public EmpresaDto(Empresa empresa) {
        this.id = empresa.getId();
        this.nombre = empresa.getNombre();
        this.descripcion = empresa.getDescripcion();
    }

    public EmpresaDto(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public EmpresaDto(String nombre) {
        this.nombre = nombre;
    }

}

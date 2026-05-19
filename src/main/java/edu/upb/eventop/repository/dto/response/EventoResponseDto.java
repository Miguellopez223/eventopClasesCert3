package edu.upb.eventop.repository.dto.response;

import edu.upb.eventop.repository.entity.Eventos;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventoResponseDto {
    private String id;
    private String nombre;
    private String descripcion;
    private EmpresaDto empresa;
    public EventoResponseDto() {

    }

    public EventoResponseDto(String id, String nombre, String descripcion, EmpresaDto empresa) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.empresa = empresa;
    }

    public EventoResponseDto(String id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public EventoResponseDto(Eventos eventos) {
        this.id = eventos.getId();
        this.nombre = eventos.getNombre();
        this.descripcion = eventos.getDescripcion();
        this.empresa = new EmpresaDto(eventos.getEmpresa());
    }
}

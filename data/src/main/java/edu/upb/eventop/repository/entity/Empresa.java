package edu.upb.eventop.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "empresas")
public class Empresa extends AuditableEntity{

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "nombre", length = 50, nullable = false,
            comment = "Esta columna almacena el nombre del evento")
    private String nombreEmpresa;

    @Column(name = "descrpcion", length = 250,
            comment = "Esta columna almacena la descripcion del evento")
    private String descripcion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombreEmpresa;
    }

    public void setNombre(String nombre) {
        this.nombreEmpresa = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}

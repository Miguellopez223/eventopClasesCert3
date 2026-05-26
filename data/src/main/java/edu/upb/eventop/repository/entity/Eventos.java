package edu.upb.eventop.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "eventos")
public class Eventos extends AuditableEntity {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "nombre", length = 50, nullable = false,
            comment = "Esta columna almacena el nombre del evento")
    private String nombre;

    @Column(name = "descrpcion", length = 250,
            comment = "Esta columna almacena la descripcion del evento")
    private String descripcion;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", referencedColumnName = "id")
    private Empresa materia;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Empresa getEmpresa() {
        return materia;
    }

    public void setEmpresa(Empresa empresa) {
        this.materia = empresa;
    }
}

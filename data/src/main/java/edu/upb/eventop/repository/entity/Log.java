package edu.upb.eventop.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "nivel", length = 10,
            comment = "Nivel del log: INFO, ERROR, etc.")
    private String nivel;

    @Column(name = "mensaje", length = 1000,
            comment = "Mensaje del log")
    private String mensaje;

    @Column(name = "usuario", length = 100,
            comment = "Usuario que originó la operación")
    private String usuario;

    @Column(name = "fecha", nullable = false,
            comment = "Fecha y hora de registro del log")
    private LocalDateTime fecha;
}

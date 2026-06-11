package edu.upb.eventop.repository.entity;

import edu.upb.eventop.repository.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;


@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "log")
public class Log extends AuditableEntity{

    @Id
    @UuidGenerator
    private String id;

    @Column(name = "_level", length = 10 )
    @Enumerated(EnumType.STRING)
    private LogLevel level;

    @Column(name = "_message", length = 4000, comment = "Esta columna almacena el log")
    private String message;

}

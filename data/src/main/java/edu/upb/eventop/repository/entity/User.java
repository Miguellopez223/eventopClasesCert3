package edu.upb.eventop.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.upb.eventop.repository.enums.RoleType;
import edu.upb.eventop.repository.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "_user", indexes = {
        @Index(name = "idx_username", columnList = "username")}, comment = "Esta tabla es para almacenar informacion de usuario")
public class User extends AuditableEntity implements UserDetails  {
    @Comment("Identificador de tabla")
    @Id
    @UuidGenerator
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "username", length = 200, nullable = false)
    private String username;


    @Comment("Nombre del usuario")
    @Column(name = "nombres", length = 60)
    private String nombres;

    @Comment("Segundo Apellido")
    @Column(name = "apellidos", length = 60)
    private String apellidos;


    @Comment("Dirección de correo electrónico")
    @Column(name = "email", length = 200)
    private String email;


    @Comment("Rol del usuario")
    @Column(name = "rol", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Comment("Estado del usuario")
    @Column(name = "estado", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;


    public User(String id, String username, RoleType role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.name()));
        return authorities;
    }


    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status == UserStatus.ACTIVO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}

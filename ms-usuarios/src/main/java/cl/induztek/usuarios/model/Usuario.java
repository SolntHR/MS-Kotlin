package cl.induztek.usuarios.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "correo_personal", nullable = false, unique = true, length = 100)
    private String correoPersonal;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(name = "password_hash", length = 64)
    private String passwordHash;

    @Column(name = "password_sal", length = 32)
    private String passwordSal;

    @Column(nullable = false)
    private boolean activo;

    @Column(length = 100)
    private String giro;

    @Column(name = "contacto_nombre", length = 100)
    private String contactoNombre;

    @Column(name = "contacto_cargo", length = 100)
    private String contactoCargo;

    @Column(name = "pregunta_seguridad", length = 150)
    private String preguntaSeguridad;

    @Column(name = "respuesta_hash", length = 64)
    private String respuestaHash;

    @Column(name = "respuesta_sal", length = 32)
    private String respuestaSal;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public boolean tieneContrasena() {
        return passwordHash != null;
    }

    @PrePersist
    void antesDeGuardar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}

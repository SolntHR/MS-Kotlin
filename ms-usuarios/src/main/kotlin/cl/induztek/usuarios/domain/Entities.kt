package cl.induztek.usuarios.domain

import jakarta.persistence.*
import java.time.Instant

enum class Rol { ADMIN, TECNICO, INGENIERO, JEFE_TECNICO, CLIENTE }

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    var nombre: String = "",
    var rut: String = "",
    var telefono: String = "",
    var correoPersonal: String = "",
    var correoInterno: String = "",
    var preguntaSeguridad: String = "",
    var respuestaHash: String = "",
    var passwordHash: String? = null,
    @Enumerated(EnumType.STRING) var rolCodigo: Rol? = null,
    var activo: Boolean = false,
    var creadoEn: Instant = Instant.now()
)

@Entity
@Table(name = "sesiones")
class Sesion(
    @Id var tokenHash: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id") var usuario: Usuario = Usuario(),
    var expiraEn: Instant = Instant.now()
)

@Entity
@Table(name = "codigos_recuperacion")
class CodigoRecuperacion(
    @Id var usuarioId: Long = 0,
    var codigoHash: String = "",
    var expiraEn: Instant = Instant.now(),
    var intentos: Int = 0,
    var enviadoEn: Instant = Instant.now()
)

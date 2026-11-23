package cl.induztek.usuarios

import cl.induztek.usuarios.api.*
import cl.induztek.usuarios.domain.*
import cl.induztek.usuarios.service.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import java.time.*
import java.util.UUID

class MutableClock(private var value: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock() {
    override fun instant(): Instant = value
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    fun advance(duration: Duration) { value = value.plus(duration) }
}

@TestConfiguration
class TestTimeConfig {
    @Bean @Primary fun testClock() = MutableClock()
}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestTimeConfig::class)
abstract class ApiTestBase {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var usuarios: UsuarioRepository
    @Autowired lateinit var sesiones: SesionRepository
    @Autowired lateinit var codigos: CodigoRepository
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var service: UsuarioService
    @Autowired lateinit var encoder: PasswordEncoder
    @Autowired lateinit var secrets: SecretService
    @Autowired lateinit var clock: MutableClock
    @MockitoBean lateinit var sender: JavaMailSender
    lateinit var admin: Usuario
    val clave = "Una clave segura 2026"
    val pregunta = "¿Cuál es el nombre de tu primera mascota?"

    @BeforeEach
    fun preparar() {
        clock.advance(Duration.ofDays(1))
        sesiones.deleteAllInBatch()
        codigos.deleteAllInBatch()
        usuarios.deleteAllInBatch()
        admin = cuenta(Rol.ADMIN, "111111111")
    }
    fun cuenta(rol: Rol? = Rol.TECNICO, rut: String = "123456785", activo: Boolean = true, password: Boolean = true): Usuario {
        val correo = "${UUID.randomUUID()}@induztek.ejemplo"
        return usuarios.saveAndFlush(Usuario(
            nombre = "Ana Pérez Soto", rut = rut, telefono = "+56 9 1234 5678", correoPersonal = "${UUID.randomUUID()}@example.org", correoInterno = correo,
            preguntaSeguridad = pregunta, respuestaHash = encoder.encode("luna"),
            passwordHash = if (password) encoder.encode(clave) else null, rolCodigo = rol, activo = activo, creadoEn = clock.instant()
        ))
    }
    fun token(usuario: Usuario = admin) = auth.login(LoginRequest(usuario.correoInterno, clave)).accessToken
    fun registro(correo: String = "ana@example.org", rut: String = "12.345.678-5") = RegistroRequest("Ana Pérez Soto", rut, "+56 9 1234 5678", correo, pregunta, "Luna")
    fun json(value: Any): String = mapper.writeValueAsString(value)
}

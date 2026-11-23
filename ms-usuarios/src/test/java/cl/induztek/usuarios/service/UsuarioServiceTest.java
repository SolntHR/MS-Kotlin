package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.usuarios.dto.ActualizarUsuarioRequest;
import cl.induztek.usuarios.dto.CrearUsuarioRequest;
import cl.induztek.usuarios.dto.LoginRequest;
import cl.induztek.usuarios.dto.RegistroRequest;
import cl.induztek.usuarios.dto.RegistroResponse;
import cl.induztek.usuarios.dto.UsuarioResponse;
import cl.induztek.usuarios.exception.AccesoDenegadoException;
import cl.induztek.usuarios.exception.ConflictoException;
import cl.induztek.usuarios.exception.CredencialesInvalidasException;
import cl.induztek.usuarios.exception.DemasiadosIntentosException;
import cl.induztek.usuarios.exception.RecursoNoEncontradoException;
import cl.induztek.usuarios.exception.SolicitudInvalidaException;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String PREGUNTA = "¿Cuál es tu comida favorita?";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CodigoService codigoService;

    @Spy
    private HashService hashService = new HashService();

    private UsuarioService usuarioService;

    private Usuario matias;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
        usuarioService = new UsuarioService(usuarioRepository, hashService, codigoService,
                new GeneradorCorreo(usuarioRepository, "induztek.ejemplo"), new LimiteIntentos(reloj));
        matias = Usuario.builder().id(1L).rol(Rol.TECNICO).rut("17.284.519-3").nombre("Matías Godoy Herrera")
                .correo("mgodoy@induztek.ejemplo").correoPersonal("matias.godoy@gmail.com")
                .telefono("+56 9 8123 4567").activo(true).build();
        usuarioService.asignarContrasena(matias, "123456");
    }

    @Test
    void loginCorrectoDevuelveElUsuarioSinContrasena() {
        when(usuarioRepository.findByCorreoIgnoreCase("mgodoy@induztek.ejemplo")).thenReturn(Optional.of(matias));

        UsuarioResponse respuesta = usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456"));

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.rolCodigo()).isEqualTo("TECNICO");
        assertThat(respuesta.rolNombre()).isEqualTo("Técnico de terreno");
    }

    @Test
    void loginConContrasenaIncorrectaLanza401() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(matias));

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "999999")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void loginConCorreoInexistenteLanza401() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("nadie@induztek.ejemplo", "123456")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void loginDeCuentaSinContrasenaSimulaLaVerificacionYLanza401() {
        matias.setPasswordHash(null);
        matias.setPasswordSal(null);
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(matias));

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(hashService).simularVerificacion("123456");
    }

    @Test
    void loginSeBloqueaTrasDiezIntentosFallidosYSeReiniciaConUnoCorrecto() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(matias));
        for (int i = 0; i < 9; i++) {
            assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "999999")))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456"));
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> usuarioService.login(new LoginRequest("MGODOY@induztek.ejemplo", "999999")))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }

        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456")))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void loginDeCuentaDesactivadaOSinRolLanza403() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.of(matias));
        matias.setActivo(false);
        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456")))
                .isInstanceOf(AccesoDenegadoException.class).hasMessageContaining("desactivada");

        matias.setActivo(true);
        matias.setRol(Rol.SIN_ASIGNAR);
        assertThatThrownBy(() -> usuarioService.login(new LoginRequest("mgodoy@induztek.ejemplo", "123456")))
                .isInstanceOf(AccesoDenegadoException.class).hasMessageContaining("perfil");
    }

    @Test
    void registroGeneraElCorreoDeAccesoYQuedaPendienteSinContrasena() {
        when(usuarioRepository.existsByCorreoIgnoreCase("atorres@induztek.ejemplo")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("atorres2@induztek.ejemplo")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        RegistroResponse respuesta = usuarioService.registrar(new RegistroRequest("  Ana   Torres Soto ", "15906334-8",
                "Ana.Torres@Gmail.com", "+56 9 1234 5678", PREGUNTA, "Pizza"));

        assertThat(respuesta.id()).isEqualTo(20L);
        assertThat(respuesta.correoInterno()).isEqualTo("atorres2@induztek.ejemplo");
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE_ASIGNACION_ROL");
        assertThat(respuesta.mensaje()).contains("ana.torres@gmail.com");
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.SIN_ASIGNAR);
        assertThat(guardado.getValue().getNombre()).isEqualTo("Ana Torres Soto");
        assertThat(guardado.getValue().getRut()).isEqualTo("15.906.334-8");
        assertThat(guardado.getValue().tieneContrasena()).isFalse();
        verify(codigoService, never()).enviarActivacion(any());
    }

    @Test
    void registroRechazaCorreoPersonalRepetidoYPreguntaInventada() {
        when(usuarioRepository.existsByCorreoPersonalIgnoreCase("ana@gmail.com")).thenReturn(true);
        RegistroRequest repetido = new RegistroRequest("Ana Torres", "15906334-8", "ana@gmail.com",
                "+56 9 1234 5678", PREGUNTA, "pizza");
        assertThatThrownBy(() -> usuarioService.registrar(repetido)).isInstanceOf(ConflictoException.class);

        RegistroRequest preguntaInventada = new RegistroRequest("Ana Torres", "15906334-8", "otra@gmail.com",
                "+56 9 1234 5678", "¿Color favorito?", "azul");
        assertThatThrownBy(() -> usuarioService.registrar(preguntaInventada))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void registroRechazaRutRepetido() {
        when(usuarioRepository.existsByRutIgnoreCase("15.906.334-8")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(new RegistroRequest("Ana Torres", "15906334-8",
                "ana@gmail.com", "+56 9 1234 5678", PREGUNTA, "pizza")))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("RUT");
    }

    @Test
    void registroSeLimitaACincoIntentosPorCorreo() {
        when(usuarioRepository.existsByCorreoPersonalIgnoreCase("ana@gmail.com")).thenReturn(true);
        RegistroRequest repetido = new RegistroRequest("Ana Torres", "15906334-8", "ana@gmail.com",
                "+56 9 1234 5678", PREGUNTA, "pizza");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> usuarioService.registrar(repetido)).isInstanceOf(ConflictoException.class);
        }

        assertThatThrownBy(() -> usuarioService.registrar(repetido)).isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void elJefeCreaUnaCuentaConRolYSeEnviaElCodigoDeActivacion() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(12L);
            return u;
        });

        UsuarioResponse respuesta = usuarioService.crear(new CrearUsuarioRequest(Rol.INGENIERO, "Pedro Rojas",
                "15906334-8", "pedro.rojas@gmail.com", "+56 9 1111 2222", " ", null, null, PREGUNTA, "porotos"));

        assertThat(respuesta.rolCodigo()).isEqualTo("INGENIERO");
        assertThat(respuesta.correo()).isEqualTo("projas@induztek.ejemplo");
        assertThat(respuesta.correoPersonal()).isEqualTo("pedro.rojas@gmail.com");
        assertThat(respuesta.tieneContrasena()).isFalse();
        assertThat(respuesta.giro()).isNull();
        verify(codigoService).enviarActivacion(any(Usuario.class));
    }

    @Test
    void elJefeNoPuedeCrearNiAsignarUnaCuentaSinRol() {
        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(Rol.SIN_ASIGNAR, "Pedro Rojas",
                "15906334-8", "p@gmail.com", "+56 9 1111 2222", null, null, null, PREGUNTA, "x")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> usuarioService.cambiarRol(1L, Rol.SIN_ASIGNAR))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void listarAplicaLosFiltros() {
        when(usuarioRepository.buscar(Rol.CLIENTE, true, null)).thenReturn(List.of(matias));
        when(usuarioRepository.buscar(null, null, "godoy")).thenReturn(List.of(matias));

        assertThat(usuarioService.listar(Rol.CLIENTE, true, "  ")).hasSize(1);
        assertThat(usuarioService.listar(null, null, " godoy ")).hasSize(1);
    }

    @Test
    void obtenerUsuarioInexistenteLanza404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarCambiaLosDatosYValidaElRut() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(matias));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = usuarioService.actualizar(1L, new ActualizarUsuarioRequest("Matías Godoy",
                "17284519-3", "+56 9 8123 0000", null, "Contacto", "Cargo"));

        assertThat(respuesta.nombre()).isEqualTo("Matías Godoy");
        assertThat(respuesta.contactoNombre()).isEqualTo("Contacto");

        when(usuarioRepository.existsByRutIgnoreCaseAndIdNot("17.284.519-3", 1L)).thenReturn(true);
        assertThatThrownBy(() -> usuarioService.actualizar(1L, new ActualizarUsuarioRequest("Matías", "17284519-3",
                "+56 9 8123 0000", null, null, null))).isInstanceOf(ConflictoException.class);
    }

    @Test
    void cambiarRolYEstado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(matias));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.cambiarRol(1L, Rol.INGENIERO).rolCodigo()).isEqualTo("INGENIERO");
        assertThat(usuarioService.cambiarActivo(1L, false).activo()).isFalse();
        verify(codigoService, never()).enviarActivacion(any());
    }

    @Test
    void asignarRolAUnaCuentaPendienteEnviaElCodigoDeActivacion() {
        matias.setRol(Rol.SIN_ASIGNAR);
        matias.setPasswordHash(null);
        matias.setPasswordSal(null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(matias));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.cambiarRol(1L, Rol.TECNICO).rolCodigo()).isEqualTo("TECNICO");

        verify(codigoService).enviarActivacion(matias);
    }

    @Test
    void cambiarContrasenaExigeLaActualYUnaDistinta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(matias));

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(1L, "malas", "nueva123"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> usuarioService.cambiarContrasena(1L, "123456", "123456"))
                .isInstanceOf(SolicitudInvalidaException.class);

        usuarioService.cambiarContrasena(1L, "123456", "nueva123");

        assertThat(hashService.coincide("nueva123", matias.getPasswordSal(), matias.getPasswordHash())).isTrue();
    }

    @Test
    void entregaLasPreguntasDeSeguridad() {
        assertThat(usuarioService.preguntasSeguridad()).hasSize(5).contains(PREGUNTA);
    }
}

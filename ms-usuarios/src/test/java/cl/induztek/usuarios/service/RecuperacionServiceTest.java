package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.induztek.usuarios.exception.DemasiadosIntentosException;
import cl.induztek.usuarios.exception.SolicitudInvalidaException;
import cl.induztek.usuarios.model.CodigoRecuperacion;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.CodigoRecuperacionRepository;
import cl.induztek.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    private static final String CORREO = "mgodoy@induztek.ejemplo";
    private static final String PERSONAL = "matias.godoy@gmail.com";
    private static final String PREGUNTA = "¿Cuál es el nombre de tu primera mascota?";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CodigoRecuperacionRepository codigoRepository;

    @Mock
    private CorreoService correoService;

    private final HashService hashService = new HashService();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneId.of("UTC"));
    private RecuperacionService recuperacionService;
    private CodigoService codigoService;
    private Usuario matias;

    @BeforeEach
    void preparar() {
        LimiteIntentos limiteIntentos = new LimiteIntentos(reloj);
        codigoService = new CodigoService(codigoRepository, hashService, correoService, reloj);
        UsuarioService usuarioService = new UsuarioService(usuarioRepository, hashService, codigoService,
                new GeneradorCorreo(usuarioRepository, "induztek.ejemplo"), limiteIntentos);
        recuperacionService = new RecuperacionService(usuarioRepository, codigoRepository, hashService,
                codigoService, usuarioService, limiteIntentos, reloj);
        String sal = hashService.generarSal();
        matias = Usuario.builder().id(1L).rol(Rol.TECNICO).correo(CORREO).correoPersonal(PERSONAL).nombre("Matías")
                .activo(true).preguntaSeguridad(PREGUNTA).respuestaSal(sal)
                .respuestaHash(hashService.hash("firulais", sal)).build();
        usuarioService.asignarContrasena(matias, "123456");
    }

    @Test
    void laPreguntaYRespuestaCorrectasPermitenCambiarLaContrasena() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));

        recuperacionService.verificarPregunta(CORREO, PREGUNTA, " Firulais ");
        recuperacionService.restablecerConPregunta(CORREO, PREGUNTA, "firulais", "nueva123");

        assertThat(hashService.coincide("nueva123", matias.getPasswordSal(), matias.getPasswordHash())).isTrue();
        verify(usuarioRepository).save(matias);
    }

    @Test
    void tambienSePuedeRecuperarConElCorreoPersonal() {
        when(usuarioRepository.findByCorreoIgnoreCase(PERSONAL)).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoPersonalIgnoreCase(PERSONAL)).thenReturn(Optional.of(matias));

        recuperacionService.restablecerConPregunta(PERSONAL, PREGUNTA, "firulais", "nueva123");

        assertThat(hashService.coincide("nueva123", matias.getPasswordSal(), matias.getPasswordHash())).isTrue();
    }

    @Test
    void respuestaPreguntaOCorreoIncorrectosNoRevelanCualFallo() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(usuarioRepository.findByCorreoIgnoreCase("otro@induztek.ejemplo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, PREGUNTA, "rocky"))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage(RecuperacionService.DATOS_NO_COINCIDEN);
        assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, "¿Cuál es tu comida favorita?", "firulais"))
                .hasMessage(RecuperacionService.DATOS_NO_COINCIDEN);
        assertThatThrownBy(() -> recuperacionService.verificarPregunta("otro@induztek.ejemplo", PREGUNTA, "firulais"))
                .hasMessage(RecuperacionService.DATOS_NO_COINCIDEN);
    }

    @Test
    void unaCuentaDesactivadaOPendienteNoPuedeRecuperarConPregunta() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        matias.setActivo(false);
        assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, PREGUNTA, "firulais"))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("desactivada");

        matias.setActivo(true);
        matias.setRol(Rol.SIN_ASIGNAR);
        assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, PREGUNTA, "firulais"))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("perfil");
    }

    @Test
    void laPreguntaSeBloqueaTrasDiezIntentos() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, PREGUNTA, "rocky"))
                    .isInstanceOf(SolicitudInvalidaException.class);
        }

        assertThatThrownBy(() -> recuperacionService.verificarPregunta(CORREO, PREGUNTA, "firulais"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void solicitarCodigoGuardaSoloElHashYLoEnviaAlCorreoPersonal() {
        CodigoRecuperacion anterior = CodigoRecuperacion.builder().usado(false).build();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(codigoRepository.findByUsuarioAndUsadoFalse(matias)).thenReturn(List.of(anterior));

        recuperacionService.solicitarCodigo(CORREO);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigoRecuperacion(eq(PERSONAL), eq("Matías"), codigo.capture());
        ArgumentCaptor<CodigoRecuperacion> guardado = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRepository, times(2)).save(guardado.capture());
        CodigoRecuperacion nuevo = guardado.getAllValues().get(1);
        assertThat(anterior.isUsado()).isTrue();
        assertThat(nuevo.getCodigoHash()).isNotEqualTo(codigo.getValue());
        assertThat(hashService.coincide(codigo.getValue(), nuevo.getCodigoSal(), nuevo.getCodigoHash())).isTrue();
        assertThat(nuevo.getExpiraEn()).isEqualTo(LocalDateTime.of(2026, 11, 23, 12, 10));
        assertThat(nuevo.getCreadoEn()).isEqualTo(LocalDateTime.of(2026, 11, 23, 12, 0));
    }

    @Test
    void noSeEnviaOtroCodigoAntesDeUnMinuto() {
        CodigoRecuperacion reciente = CodigoRecuperacion.builder().usado(false)
                .creadoEn(LocalDateTime.of(2026, 11, 23, 11, 59, 30)).build();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(matias)).thenReturn(Optional.of(reciente));

        recuperacionService.solicitarCodigo(CORREO);

        verify(correoService, never()).enviarCodigoRecuperacion(anyString(), anyString(), anyString());
        reciente.setCreadoEn(LocalDateTime.of(2026, 11, 23, 11, 59));
        recuperacionService.solicitarCodigo(CORREO);
        verify(correoService).enviarCodigoRecuperacion(eq(PERSONAL), eq("Matías"), anyString());
    }

    @Test
    void solicitarCodigoParaCorreoDesconocidoOCuentaPendienteNoEnviaNada() {
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@induztek.ejemplo")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoPersonalIgnoreCase("nadie@induztek.ejemplo")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        matias.setRol(Rol.SIN_ASIGNAR);

        recuperacionService.solicitarCodigo("nadie@induztek.ejemplo");
        recuperacionService.solicitarCodigo(CORREO);

        verify(correoService, never()).enviarCodigoRecuperacion(anyString(), anyString(), anyString());
    }

    @Test
    void solicitarCodigoSeBloqueaTrasCincoPedidos() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            recuperacionService.solicitarCodigo("nadie@induztek.ejemplo");
        }

        assertThatThrownBy(() -> recuperacionService.solicitarCodigo("nadie@induztek.ejemplo"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void elCodigoDeActivacionVaAlCorreoPersonalConElCorreoDeAccesoYDura24Horas() {
        codigoService.enviarActivacion(matias);

        verify(correoService).enviarCodigoActivacion(eq(PERSONAL), eq("Matías"), eq(CORREO), anyString());
        ArgumentCaptor<CodigoRecuperacion> guardado = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(LocalDateTime.of(2026, 11, 24, 12, 0));
    }

    @Test
    void codigoCorrectoYVigenteRestableceLaContrasenaYQuedaUsado() {
        CodigoRecuperacion registro = codigo("482913", LocalDateTime.of(2026, 11, 23, 12, 5), 0);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(matias)).thenReturn(Optional.of(registro));

        recuperacionService.verificarCodigo(CORREO, "482913");
        recuperacionService.restablecerConCodigo(CORREO, "482913", "nueva123");

        assertThat(registro.isUsado()).isTrue();
        assertThat(hashService.coincide("nueva123", matias.getPasswordSal(), matias.getPasswordHash())).isTrue();
    }

    @Test
    void codigoIncorrectoSumaUnIntento() {
        CodigoRecuperacion registro = codigo("482913", LocalDateTime.of(2026, 11, 23, 12, 5), 0);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(matias)).thenReturn(Optional.of(registro));

        assertThatThrownBy(() -> recuperacionService.verificarCodigo(CORREO, "000000"))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThat(registro.getIntentos()).isEqualTo(1);
    }

    @Test
    void codigoVencidoOConDemasiadosIntentosNoSirve() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(matias));
        when(codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(matias))
                .thenReturn(Optional.of(codigo("482913", LocalDateTime.of(2026, 11, 23, 11, 59), 0)))
                .thenReturn(Optional.of(codigo("482913", LocalDateTime.of(2026, 11, 23, 12, 5), 5)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recuperacionService.verificarCodigo(CORREO, "482913"))
                .hasMessage(RecuperacionService.CODIGO_INVALIDO);
        assertThatThrownBy(() -> recuperacionService.verificarCodigo(CORREO, "482913"))
                .hasMessage(RecuperacionService.CODIGO_INVALIDO);
        assertThatThrownBy(() -> recuperacionService.verificarCodigo(CORREO, "482913"))
                .hasMessage(RecuperacionService.CODIGO_INVALIDO);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void codigoDeCorreoDesconocidoNoSirve() {
        when(usuarioRepository.findByCorreoIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recuperacionService.restablecerConCodigo("x@induztek.ejemplo", "123456", "nueva123"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private CodigoRecuperacion codigo(String valor, LocalDateTime expira, int intentos) {
        String sal = hashService.generarSal();
        return CodigoRecuperacion.builder().id(5L).usuario(matias).codigoSal(sal)
                .codigoHash(hashService.hash(valor, sal)).expiraEn(expira).intentos(intentos).usado(false)
                .creadoEn(expira.minusMinutes(10)).build();
    }
}

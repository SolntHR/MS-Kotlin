package cl.induztek.usuarios.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.usuarios.exception.SolicitudInvalidaException;
import cl.induztek.usuarios.model.CodigoRecuperacion;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.CodigoRecuperacionRepository;
import cl.induztek.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecuperacionService {

    static final int INTENTOS_MAXIMOS = 5;
    static final int MAXIMO_SOLICITUDES = 5;
    static final int MAXIMO_VERIFICACIONES = 10;
    static final String DATOS_NO_COINCIDEN =
            "Los datos no coinciden con ninguna cuenta. Revisa el correo, la pregunta y la respuesta.";
    static final String CODIGO_INVALIDO = "El código no es válido o ya venció. Solicita uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRepository;
    private final HashService hashService;
    private final CodigoService codigoService;
    private final UsuarioService usuarioService;
    private final LimiteIntentos limiteIntentos;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public void verificarPregunta(String correo, String pregunta, String respuesta) {
        limiteIntentos.verificar("pregunta", correo, MAXIMO_VERIFICACIONES);
        buscarPorPregunta(correo, pregunta, respuesta);
    }

    @Transactional
    public void restablecerConPregunta(String correo, String pregunta, String respuesta, String nuevaContrasena) {
        limiteIntentos.verificar("pregunta", correo, MAXIMO_VERIFICACIONES);
        Usuario usuario = buscarPorPregunta(correo, pregunta, respuesta);
        guardarNuevaContrasena(usuario, nuevaContrasena);
    }

    @Transactional
    public void solicitarCodigo(String correo) {
        limiteIntentos.verificar("codigo", correo, MAXIMO_SOLICITUDES);
        buscarCuenta(correo)
                .filter(this::habilitada)
                .filter(codigoService::puedeEnviarOtro)
                .ifPresent(codigoService::enviarRecuperacion);
    }

    @Transactional(noRollbackFor = SolicitudInvalidaException.class)
    public void verificarCodigo(String correo, String codigo) {
        limiteIntentos.verificar("verificar-codigo", correo, MAXIMO_VERIFICACIONES);
        validarCodigo(correo, codigo);
    }

    @Transactional(noRollbackFor = SolicitudInvalidaException.class)
    public void restablecerConCodigo(String correo, String codigo, String nuevaContrasena) {
        limiteIntentos.verificar("verificar-codigo", correo, MAXIMO_VERIFICACIONES);
        CodigoRecuperacion registro = validarCodigo(correo, codigo);
        registro.setUsado(true);
        codigoRepository.save(registro);
        guardarNuevaContrasena(registro.getUsuario(), nuevaContrasena);
    }

    private Optional<Usuario> buscarCuenta(String correo) {
        String limpio = correo.trim();
        return usuarioRepository.findByCorreoIgnoreCase(limpio);
    }

    private boolean habilitada(Usuario usuario) {
        return usuario.isActivo() && usuario.getRol() != Rol.SIN_ASIGNAR;
    }

    private CodigoRecuperacion validarCodigo(String correo, String codigo) {
        Usuario usuario = buscarCuenta(correo)
                .filter(this::habilitada)
                .orElseThrow(() -> new SolicitudInvalidaException(CODIGO_INVALIDO));
        CodigoRecuperacion registro = codigoRepository.findFirstByUsuarioAndUsadoFalseOrderByIdDesc(usuario)
                .orElseThrow(() -> new SolicitudInvalidaException(CODIGO_INVALIDO));
        boolean vencido = registro.getExpiraEn().isBefore(LocalDateTime.now(reloj));
        if (vencido || registro.getIntentos() >= INTENTOS_MAXIMOS) {
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }
        if (!hashService.coincide(codigo.trim(), registro.getCodigoSal(), registro.getCodigoHash())) {
            registro.setIntentos(registro.getIntentos() + 1);
            codigoRepository.save(registro);
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }
        return registro;
    }

    private Usuario buscarPorPregunta(String correo, String pregunta, String respuesta) {
        Usuario usuario = buscarCuenta(correo).orElse(null);
        if (usuario == null) {
            hashService.simularVerificacion(respuesta);
            throw new SolicitudInvalidaException(DATOS_NO_COINCIDEN);
        }
        boolean preguntaCorrecta = usuario.getPreguntaSeguridad() != null
                && usuario.getPreguntaSeguridad().equals(pregunta.trim());
        boolean respuestaCorrecta = hashService.coincide(hashService.normalizarRespuesta(respuesta),
                usuario.getRespuestaSal(), usuario.getRespuestaHash());
        if (!preguntaCorrecta || !respuestaCorrecta) {
            throw new SolicitudInvalidaException(DATOS_NO_COINCIDEN);
        }
        if (!usuario.isActivo()) {
            throw new SolicitudInvalidaException("La cuenta está desactivada. Contacta al Jefe técnico.");
        }
        if (usuario.getRol() == Rol.SIN_ASIGNAR) {
            throw new SolicitudInvalidaException("Tu cuenta aún no tiene un perfil asignado por el Jefe técnico.");
        }
        return usuario;
    }

    private void guardarNuevaContrasena(Usuario usuario, String nuevaContrasena) {
        usuarioService.asignarContrasena(usuario, nuevaContrasena);
        usuarioRepository.save(usuario);
    }
}

package cl.induztek.usuarios.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.usuarios.dto.ActualizarUsuarioRequest;
import cl.induztek.usuarios.dto.CrearUsuarioRequest;
import cl.induztek.usuarios.dto.LoginRequest;
import cl.induztek.usuarios.dto.RegistroRequest;
import cl.induztek.usuarios.dto.RegistroResponse;
import cl.induztek.usuarios.dto.UsuarioResponse;
import cl.induztek.usuarios.exception.AccesoDenegadoException;
import cl.induztek.usuarios.exception.ConflictoException;
import cl.induztek.usuarios.exception.CredencialesInvalidasException;
import cl.induztek.usuarios.exception.RecursoNoEncontradoException;
import cl.induztek.usuarios.exception.SolicitudInvalidaException;
import cl.induztek.usuarios.model.PreguntasSeguridad;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;
import cl.induztek.usuarios.repository.UsuarioRepository;
import cl.induztek.usuarios.validacion.ValidadorRut;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";
    static final int MAXIMO_LOGIN = 10;
    static final int MAXIMO_REGISTRO = 5;

    private final UsuarioRepository usuarioRepository;
    private final HashService hashService;
    private final CodigoService codigoService;
    private final GeneradorCorreo generadorCorreo;
    private final LimiteIntentos limiteIntentos;

    @Transactional(readOnly = true)
    public UsuarioResponse login(LoginRequest request) {
        String correo = request.correo().trim();
        limiteIntentos.verificar("login", correo, MAXIMO_LOGIN);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);
        if (usuario == null || !usuario.tieneContrasena()) {
            hashService.simularVerificacion(request.contrasena());
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }
        if (!hashService.coincide(request.contrasena(), usuario.getPasswordSal(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }
        if (!usuario.isActivo()) {
            throw new AccesoDenegadoException("Tu cuenta está desactivada. Contacta al Jefe técnico.");
        }
        if (usuario.getRol() == Rol.SIN_ASIGNAR) {
            throw new AccesoDenegadoException("Tu cuenta aún no tiene un perfil asignado. Pídele al Jefe técnico que lo asigne.");
        }
        limiteIntentos.reiniciar("login", correo);
        return UsuarioResponse.desde(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol, Boolean activo, String buscar) {
        String texto = buscar == null || buscar.isBlank() ? null : buscar.trim();
        return usuarioRepository.buscar(rol, activo, texto).stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscarPorId(id));
    }

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        String correoPersonal = request.correoPersonal().trim().toLowerCase();
        limiteIntentos.verificar("registro", correoPersonal, MAXIMO_REGISTRO);
        Usuario usuario = nuevoUsuario(Rol.SIN_ASIGNAR, request.nombre(), request.rut(), correoPersonal,
                request.telefono(), request.preguntaSeguridad(), request.respuestaSeguridad());
        Usuario guardado = usuarioRepository.save(usuario);
        return new RegistroResponse(guardado.getId(), guardado.getCorreo(), "PENDIENTE_ASIGNACION_ROL",
                "Tu cuenta quedó pendiente. Cuando el Jefe técnico te asigne un perfil, recibirás en "
                        + correoPersonal + " un código para crear tu contraseña.");
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (request.rol() == Rol.SIN_ASIGNAR) {
            throw new SolicitudInvalidaException("Elige un perfil para la cuenta");
        }
        Usuario usuario = nuevoUsuario(request.rol(), request.nombre(), request.rut(),
                request.correoPersonal().trim().toLowerCase(), request.telefono(), request.preguntaSeguridad(),
                request.respuestaSeguridad());
        usuario.setGiro(textoOpcional(request.giro()));
        usuario.setContactoNombre(textoOpcional(request.contactoNombre()));
        usuario.setContactoCargo(textoOpcional(request.contactoCargo()));
        Usuario guardado = usuarioRepository.save(usuario);
        codigoService.enviarActivacion(guardado);
        return UsuarioResponse.desde(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);
        String rut = ValidadorRut.formatear(request.rut());
        validarRutLibre(rut, id);
        usuario.setNombre(request.nombre().trim());
        usuario.setRut(rut);
        usuario.setTelefono(request.telefono().trim());
        usuario.setGiro(textoOpcional(request.giro()));
        usuario.setContactoNombre(textoOpcional(request.contactoNombre()));
        usuario.setContactoCargo(textoOpcional(request.contactoCargo()));
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Rol rol, Long jefeId, String contrasena) {
        if (rol == Rol.SIN_ASIGNAR) {
            throw new SolicitudInvalidaException("Elige un perfil para la cuenta");
        }
        Usuario jefe = buscarPorId(jefeId);
        if (jefe.getRol() != Rol.JEFE_TECNICO || !jefe.isActivo()) {
            throw new AccesoDenegadoException("Solo un jefe técnico puede cambiar perfiles");
        }
        if (!jefe.tieneContrasena() || !hashService.coincide(contrasena, jefe.getPasswordSal(), jefe.getPasswordHash())) {
            throw new CredencialesInvalidasException("La contraseña no es correcta");
        }
        if (jefe.getId().equals(id)) {
            throw new SolicitudInvalidaException("No puedes cambiar tu propio perfil");
        }
        Usuario usuario = buscarPorId(id);
        boolean estabaPendiente = usuario.getRol() == Rol.SIN_ASIGNAR;
        if (usuario.getRol() == Rol.CLIENTE || (rol == Rol.CLIENTE && !estabaPendiente)) {
            throw new SolicitudInvalidaException("El personal y los clientes no pueden intercambiar perfiles");
        }
        usuario.setRol(rol);
        Usuario guardado = usuarioRepository.save(usuario);
        if (estabaPendiente && !guardado.tieneContrasena()) {
            codigoService.enviarActivacion(guardado);
        }
        return UsuarioResponse.desde(guardado);
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(activo);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void cambiarContrasena(Long id, String actual, String nueva) {
        Usuario usuario = buscarPorId(id);
        if (!hashService.coincide(actual, usuario.getPasswordSal(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new SolicitudInvalidaException("La nueva contraseña debe ser distinta a la actual");
        }
        asignarContrasena(usuario, nueva);
        usuarioRepository.save(usuario);
    }

    public List<String> preguntasSeguridad() {
        return PreguntasSeguridad.LISTA;
    }

    void asignarContrasena(Usuario usuario, String contrasena) {
        String sal = hashService.generarSal();
        usuario.setPasswordSal(sal);
        usuario.setPasswordHash(hashService.hash(contrasena, sal));
    }

    private Usuario nuevoUsuario(Rol rol, String nombre, String rutIngresado, String correoPersonal, String telefono,
            String pregunta, String respuesta) {
        boolean conPregunta = pregunta != null && !pregunta.isBlank();
        if (conPregunta && !PreguntasSeguridad.esValida(pregunta)) {
            throw new SolicitudInvalidaException("Elige una de las preguntas de seguridad disponibles");
        }
        if (conPregunta && (respuesta == null || respuesta.isBlank())) {
            throw new SolicitudInvalidaException("Escribe la respuesta de seguridad");
        }
        String rut = ValidadorRut.formatear(rutIngresado);
        validarRutLibre(rut, null);
        if (usuarioRepository.existsByCorreoPersonalIgnoreCase(correoPersonal)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo personal");
        }
        String nombreLimpio = nombre.trim().replaceAll("\\s+", " ");
        Usuario usuario = Usuario.builder()
                .rol(rol)
                .nombre(nombreLimpio)
                .rut(rut)
                .correo(generadorCorreo.generar(nombreLimpio))
                .correoPersonal(correoPersonal)
                .telefono(telefono.trim())
                .activo(true)
                .build();
        if (conPregunta) {
            asignarRespuesta(usuario, pregunta, respuesta);
        }
        return usuario;
    }

    private void asignarRespuesta(Usuario usuario, String pregunta, String respuesta) {
        String sal = hashService.generarSal();
        usuario.setPreguntaSeguridad(pregunta.trim());
        usuario.setRespuestaSal(sal);
        usuario.setRespuestaHash(hashService.hash(hashService.normalizarRespuesta(respuesta), sal));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    private void validarRutLibre(String rut, Long id) {
        boolean ocupado = id == null
                ? usuarioRepository.existsByRutIgnoreCase(rut)
                : usuarioRepository.existsByRutIgnoreCaseAndIdNot(rut, id);
        if (ocupado) {
            throw new ConflictoException("Ya existe una cuenta con ese RUT");
        }
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

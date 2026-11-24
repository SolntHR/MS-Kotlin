package cl.induztek.usuarios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.usuarios.dto.ActualizarUsuarioRequest;
import cl.induztek.usuarios.dto.CambiarActivoRequest;
import cl.induztek.usuarios.dto.CambiarContrasenaRequest;
import cl.induztek.usuarios.dto.CambiarRolRequest;
import cl.induztek.usuarios.dto.CrearUsuarioRequest;
import cl.induztek.usuarios.dto.LoginRequest;
import cl.induztek.usuarios.dto.RegistroRequest;
import cl.induztek.usuarios.dto.RegistroResponse;
import cl.induztek.usuarios.dto.UsuarioResponse;
import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cuentas, perfiles e inicio de sesión")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con el correo de acceso y la contraseña cifrada en la app")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }

    @PostMapping("/registro")
    @Operation(summary = "Registro con correo personal: genera el correo de acceso y queda sin perfil hasta que el Jefe técnico lo asigne")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @GetMapping("/preguntas")
    @Operation(summary = "Lista las preguntas de seguridad disponibles")
    public List<String> preguntas() {
        return usuarioService.preguntasSeguridad();
    }

    @GetMapping
    @Operation(summary = "Lista usuarios, con filtros opcionales por perfil, estado o texto")
    public ResponseEntity<List<UsuarioResponse>> listar(@RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String buscar) {
        return Respuestas.lista(usuarioService.listar(rol, activo, buscar));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario por su id")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "El Jefe técnico crea una cuenta con perfil; se envía un código de activación al correo personal")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un usuario")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Asigna o cambia el perfil con la contraseña del jefe técnico; si la cuenta estaba pendiente, envía el código de activación")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest request) {
        return usuarioService.cambiarRol(id, request.rol(), request.jefeId(), request.contrasena());
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva una cuenta")
    public UsuarioResponse cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambiarActivoRequest request) {
        return usuarioService.cambiarActivo(id, request.activo());
    }

    @PutMapping("/{id}/contrasena")
    @Operation(summary = "Cambia la contraseña con la sesión iniciada")
    public ResponseEntity<Void> cambiarContrasena(@PathVariable Long id,
            @Valid @RequestBody CambiarContrasenaRequest request) {
        usuarioService.cambiarContrasena(id, request.actual(), request.nueva());
        return ResponseEntity.noContent().build();
    }
}

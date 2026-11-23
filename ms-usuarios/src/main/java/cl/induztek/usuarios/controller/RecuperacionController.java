package cl.induztek.usuarios.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.usuarios.dto.MensajeResponse;
import cl.induztek.usuarios.dto.RestablecerConCodigoRequest;
import cl.induztek.usuarios.dto.RestablecerConPreguntaRequest;
import cl.induztek.usuarios.dto.SolicitarCodigoRequest;
import cl.induztek.usuarios.dto.VerificarCodigoRequest;
import cl.induztek.usuarios.dto.VerificarPreguntaRequest;
import cl.induztek.usuarios.service.RecuperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios/recuperacion")
@RequiredArgsConstructor
@Tag(name = "Recuperación de contraseña", description = "Por pregunta de seguridad o por código enviado al correo")
public class RecuperacionController {

    private final RecuperacionService recuperacionService;

    @PostMapping("/pregunta/verificar")
    @Operation(summary = "Comprueba correo, pregunta y respuesta de seguridad")
    public ResponseEntity<Void> verificarPregunta(@Valid @RequestBody VerificarPreguntaRequest request) {
        recuperacionService.verificarPregunta(request.correo(), request.pregunta(), request.respuesta());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pregunta")
    @Operation(summary = "Restablece la contraseña respondiendo la pregunta de seguridad")
    public ResponseEntity<Void> restablecerConPregunta(@Valid @RequestBody RestablecerConPreguntaRequest request) {
        recuperacionService.restablecerConPregunta(request.correo(), request.pregunta(), request.respuesta(),
                request.nuevaContrasena());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/codigo")
    @Operation(summary = "Envía un código de 6 dígitos al correo personal de la cuenta")
    public ResponseEntity<MensajeResponse> solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest request) {
        recuperacionService.solicitarCodigo(request.correo());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensajeResponse(
                "Si el correo pertenece a una cuenta activa, enviamos un código a su correo personal que vence en 10 minutos."));
    }

    @PostMapping("/codigo/verificar")
    @Operation(summary = "Comprueba el código recibido por correo")
    public ResponseEntity<Void> verificarCodigo(@Valid @RequestBody VerificarCodigoRequest request) {
        recuperacionService.verificarCodigo(request.correo(), request.codigo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/codigo/restablecer")
    @Operation(summary = "Restablece la contraseña con el código recibido por correo")
    public ResponseEntity<Void> restablecerConCodigo(@Valid @RequestBody RestablecerConCodigoRequest request) {
        recuperacionService.restablecerConCodigo(request.correo(), request.codigo(), request.nuevaContrasena());
        return ResponseEntity.noContent().build();
    }
}

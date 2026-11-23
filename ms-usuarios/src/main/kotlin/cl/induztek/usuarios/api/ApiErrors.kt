package cl.induztek.usuarios.api

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

class ApiException(val status: HttpStatus, val codigo: String, override val message: String, val errores: Map<String, String> = emptyMap()) : RuntimeException(message)

@RestControllerAdvice
class ApiErrors {
    private val log = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(ApiException::class)
    fun api(e: ApiException): ResponseEntity<ErrorResponse> = ResponseEntity.status(e.status).body(ErrorResponse(e.codigo, e.message, e.errores))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(
        ErrorResponse("VALIDACION", "Revisa los campos enviados", e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") })
    )

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class, ConstraintViolationException::class)
    fun formato() = ResponseEntity.badRequest().body(ErrorResponse("SOLICITUD_INVALIDA", "JSON, campos o parámetros inválidos"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflicto() = ResponseEntity.status(409).body(ErrorResponse("USUARIO_DUPLICADO", "El RUT o correo ya está registrado"))

    @ExceptionHandler(DataAccessException::class)
    fun datos() = ResponseEntity.status(503).body(ErrorResponse("SERVICIO_NO_DISPONIBLE", "Servicio temporalmente no disponible"))

    @ExceptionHandler(AccessDeniedException::class)
    fun acceso() = ResponseEntity.status(403).body(ErrorResponse("ACCESO_DENEGADO", "No tienes permisos para esta operación"))

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodo() = ResponseEntity.status(405).body(ErrorResponse("METODO_NO_PERMITIDO", "Método HTTP no permitido"))

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun tipo() = ResponseEntity.status(415).body(ErrorResponse("TIPO_NO_SOPORTADO", "Usa Content-Type application/json"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun ruta() = ResponseEntity.status(404).body(ErrorResponse("NO_ENCONTRADO", "Recurso no encontrado"))

    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception): ResponseEntity<ErrorResponse> {
        // No registrar cuerpos, credenciales, mensajes SQL ni respuestas SMTP.
        log.error("Error no controlado de tipo {}", e.javaClass.simpleName)
        return ResponseEntity.internalServerError().body(ErrorResponse("ERROR_INTERNO", "No se pudo completar la operación"))
    }
}

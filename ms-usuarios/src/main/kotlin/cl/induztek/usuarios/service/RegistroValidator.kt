package cl.induztek.usuarios.service

import cl.induztek.usuarios.api.ApiException
import cl.induztek.usuarios.api.RegistroRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.text.Normalizer
import java.util.Locale

@Component
class RegistroValidator {
    val preguntas = listOf(
        "¿Cuál es el nombre de tu primera mascota?",
        "¿En qué ciudad nacieron tus padres?",
        "¿Cuál es tu comida favorita?",
        "¿Cuál fue el modelo de tu primer auto?",
        "¿Cuál es el nombre de tu mejor amigo de la infancia?"
    )
    fun nombre(valor: String) = valor.trim().replace(Regex("\\s+"), " ")
    fun rut(valor: String) = valor.replace(".", "").replace("-", "").trim().uppercase(Locale.ROOT)
    fun correo(valor: String) = valor.trim().lowercase(Locale.ROOT)
    fun respuesta(valor: String) = nombre(valor).lowercase(Locale.ROOT)
    fun telefono(valor: String): String {
        val digitos = valor.replace(Regex("[\\s()+-]"), "").removePrefix("56")
        return if (digitos.matches(Regex("9[0-9]{8}"))) "+56 9 ${digitos.substring(1, 5)} ${digitos.takeLast(4)}" else ""
    }
    fun rutValido(valor: String): Boolean {
        val limpio = rut(valor)
        if (!limpio.matches(Regex("[0-9]{7,8}[0-9K]")) || limpio.dropLast(1).all { it == '0' }) return false
        val suma = limpio.dropLast(1).reversed().mapIndexed { i, c -> c.digitToInt() * (i % 6 + 2) }.sum()
        val esperado = when (val resto = 11 - suma % 11) { 11 -> '0'; 10 -> 'K'; else -> resto.digitToChar() }
        return limpio.last() == esperado
    }
    fun validar(r: RegistroRequest) {
        val errores = linkedMapOf<String, String>()
        val n = nombre(r.nombre)
        if (n.split(" ").count { p -> p.any { it.isLetter() } } < 2 || n.any { !it.isLetter() && it !in " '-" }) errores["nombre"] = "Incluye nombre y apellido, usando letras"
        if (!rutValido(r.rut)) errores["rut"] = "RUT inválido, revisa el dígito verificador"
        if (telefono(r.telefono).isEmpty()) errores["telefono"] = "Ingresa un celular chileno: +56 9 XXXX XXXX"
        if (r.preguntaSeguridad !in preguntas) errores["preguntaSeguridad"] = "Selecciona una pregunta válida"
        if (r.respuestaSeguridad.isBlank()) errores["respuestaSeguridad"] = "Ingresa tu respuesta"
        if (errores.isNotEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revisa los campos enviados", errores)
    }
    fun correoInterno(nombre: String, rut: String, dominio: String): String {
        val partes = nombre(nombre).split(" ")
        val apellido = partes[maxOf(1, partes.size - 2)]
        val base = Normalizer.normalize("${partes.first().first()}$apellido", Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).filter { it in 'a'..'z' }.take(40).ifEmpty { "usuario" }
        return "$base.${rut(rut).dropLast(1)}@$dominio"
    }
}

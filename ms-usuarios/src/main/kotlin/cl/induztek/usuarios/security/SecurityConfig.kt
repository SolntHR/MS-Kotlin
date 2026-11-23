package cl.induztek.usuarios.security

import cl.induztek.usuarios.api.ErrorResponse
import cl.induztek.usuarios.domain.Rol
import cl.induztek.usuarios.domain.SesionRepository
import cl.induztek.usuarios.service.RateLimiter
import cl.induztek.usuarios.service.SecretService
import cl.induztek.usuarios.api.ApiException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.time.Duration

data class UsuarioPrincipal(val id: Long, val rol: Rol)

@Component
class JsonSecurityErrors(private val mapper: ObjectMapper) {
    fun escribir(res: HttpServletResponse, status: Int, codigo: String, mensaje: String) {
        res.status = status
        res.contentType = "application/json"
        res.characterEncoding = "UTF-8"
        mapper.writeValue(res.outputStream, ErrorResponse(codigo, mensaje))
    }
}

@Component
class BearerFilter(
    private val sesiones: SesionRepository, private val secrets: SecretService,
    private val clock: Clock, private val errors: JsonSecurityErrors, private val limiter: RateLimiter
) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        // remoteAddr lo aporta el servidor. No confiar en X-Forwarded-For suministrado por el cliente.
        if (req.method == "POST" && req.servletPath.startsWith("/api/v1/auth/")) {
            try { limiter.verificar("ip", req.remoteAddr, 60, Duration.ofMinutes(1)) }
            catch (e: ApiException) { errors.escribir(res, e.status.value(), e.codigo, e.message); return }
        }
        val header = req.getHeader("Authorization")
        if (header != null) {
            if (!header.matches(Regex("Bearer [A-Za-z0-9_-]{43}"))) {
                errors.escribir(res, 401, "TOKEN_INVALIDO", "Token inválido o vencido"); return
            }
            val sesion = try { sesiones.buscarVigente(secrets.tokenHash(header.substring(7)), clock.instant()) }
            catch (_: DataAccessException) { errors.escribir(res, 503, "SERVICIO_NO_DISPONIBLE", "Servicio temporalmente no disponible"); return }
            if (sesion == null) { errors.escribir(res, 401, "TOKEN_INVALIDO", "Token inválido o vencido"); return }
            val principal = UsuarioPrincipal(sesion.usuario.id, checkNotNull(sesion.usuario.rolCodigo))
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority("ROLE_${principal.rol}")))
        }
        chain.doFilter(req, res)
    }
}

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun security(http: HttpSecurity, filter: BearerFilter, errors: JsonSecurityErrors): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .requestCache { it.disable() }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }
        .authorizeHttpRequests {
            it.requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            it.requestMatchers(HttpMethod.POST, "/api/v1/auth/registro", "/api/v1/auth/login", "/api/v1/auth/recuperacion/solicitar", "/api/v1/auth/recuperacion/restablecer").permitAll()
            it.requestMatchers("/api/v1/usuarios/me", "/api/v1/auth/logout").authenticated()
            it.requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
            it.anyRequest().denyAll()
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, res, _ -> errors.escribir(res, 401, "AUTENTICACION_REQUERIDA", "Se requiere un token Bearer válido") }
            it.accessDeniedHandler { _, res, _ -> errors.escribir(res, 403, "ACCESO_DENEGADO", "No tienes permisos para esta operación") }
        }
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}

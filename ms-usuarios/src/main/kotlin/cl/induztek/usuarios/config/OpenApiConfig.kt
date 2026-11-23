package cl.induztek.usuarios.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean fun openApi(): OpenAPI = OpenAPI()
        .info(Info().title("Induztek Usuarios y Autenticación").version("v1").description("Registro compatible con Android, roles administrados, sesiones revocables y recuperación por correo"))
        .servers(listOf(Server().url("/").description("Mismo host del servicio o gateway")))
        .components(Components().addSecuritySchemes("bearerAuth", SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("opaque")))
}

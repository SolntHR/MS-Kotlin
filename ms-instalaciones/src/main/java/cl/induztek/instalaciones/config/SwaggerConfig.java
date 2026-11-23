package cl.induztek.instalaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("ms-instalaciones - Induztek Protocolos")
                .version("1.0.0")
                .description("Regiones, comunas, instalaciones de los clientes, tipos de equipo y equipos."));
    }
}

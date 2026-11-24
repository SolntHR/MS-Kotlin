package cl.induztek.protocolo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("ms-protocolo - Induztek Protocolos")
                .version("1.0.0")
                .description("Visitas, protocolos de prueba, mediciones, flujo de estados, actas y evidencias."));
    }
}

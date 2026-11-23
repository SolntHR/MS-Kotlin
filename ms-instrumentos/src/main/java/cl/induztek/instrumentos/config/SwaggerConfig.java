package cl.induztek.instrumentos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("ms-instrumentos - Induztek Protocolos")
                .version("1.0.0")
                .description("Instrumentos de medición, calibraciones y vigencia."));
    }
}

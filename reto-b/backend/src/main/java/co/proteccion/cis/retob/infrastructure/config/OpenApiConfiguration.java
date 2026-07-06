package co.proteccion.cis.retob.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI aportesOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Aportes Voluntarios API — CIS Protección")
                .description("Registro, consulta y gestión de aportes voluntarios a un fondo.")
                .version("1.0.0"));
    }
}

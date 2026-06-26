package co.proteccion.cis.retob.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aportes Voluntarios API")
                        .description("API REST para el registro y consulta de aportes voluntarios a fondos de pensiones. " +
                                "Operaciones reguladas por la Superintendencia Financiera de Colombia (SFC).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CIS Proteccion S.A.")
                                .email("informatica5@gigha.com.co"))
                        .license(new License()
                                .name("Uso interno — CIS Proteccion S.A.")));
    }
}

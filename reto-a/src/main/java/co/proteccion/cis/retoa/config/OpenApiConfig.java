package co.proteccion.cis.retoa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reto A — API de Aportes Voluntarios")
                        .description("""
                                API REST para el registro y consulta de aportes voluntarios \
                                al fondo de pensiones de Protección S.A.

                                Implementa los siguientes controles bajo estándares SFC:
                                - Precisión numérica con BigDecimal (NUMERIC 19,2)
                                - Idempotencia por llave UUID v4
                                - Locking pesimista contra condiciones de carrera
                                - Tope mensual configurable por afiliado
                                - Marcado automático de aportes para revisión de cumplimiento
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tomás Ríos — CIS Protección S.A.")
                                .email("informatica5@gigha.com.co")));
    }
}

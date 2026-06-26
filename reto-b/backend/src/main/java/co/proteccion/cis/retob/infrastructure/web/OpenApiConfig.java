package co.proteccion.cis.retob.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aportes Voluntarios — CIS Protección S.A.")
                        .description("""
                                API REST para el registro y gestión de aportes voluntarios al fondo.

                                **Flujo principal:**
                                1. Crear un afiliado (`POST /api/afiliados`)
                                2. Registrar aportes (`POST /api/aportes`) — idempotente por `idempotenciaKey`
                                3. Consultar consolidado por periodo (`GET /api/aportes/consolidado`)
                                4. Cambiar estado de aportes en revisión (`PATCH /api/aportes/{id}/estado`)
                                5. Consultar historial de revisiones (`GET /api/aportes/{id}/revisiones`)

                                **Reglas de negocio:**
                                - Aportes > umbral de revisión quedan en estado `EN_REVISION` automáticamente
                                - La suma mensual por afiliado no puede superar el tope mensual configurado
                                - Los topes son auditables vía `GET /api/parametros/historial`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Andrés Giraldo")
                                .email("ingeniotechsoluciones@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local")
                ));
    }
}

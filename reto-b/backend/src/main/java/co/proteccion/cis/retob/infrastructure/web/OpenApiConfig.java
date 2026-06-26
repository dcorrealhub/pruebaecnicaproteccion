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

                                **Flujo principal (revisor/operador):**
                                1. Crear un afiliado (`POST /api/afiliados`) y gestionar su estado (`PATCH /api/afiliados/{id}/estado`)
                                2. Registrar aportes (`POST /api/aportes`) — idempotente por `idempotenciaKey`
                                3. Consultar consolidado por periodo (`GET /api/aportes/consolidado`)
                                4. Cambiar estado de aportes (`PATCH /api/aportes/{id}/estado`)
                                5. Consultar historial de revisiones (`GET /api/aportes/{id}/revisiones`)

                                **Flujo del afiliado:**
                                - Anular un aporte propio en estado `PENDIENTE` (`PATCH /api/aportes/{id}/anular`)

                                **Ciclo de vida del aporte:**
                                ```
                                PENDIENTE ──► EN_REVISION ──► APROBADO (terminal)
                                    │                   └────► RECHAZADO (terminal, libera cupo)
                                    └──────────────────────► APROBADO
                                    └──────────────────────► ANULADO (terminal, libera cupo — solo por el afiliado)
                                ```

                                **Reglas de negocio:**
                                - Solo afiliados en estado `ACTIVO` pueden registrar aportes
                                - El monto debe ser ≥ monto mínimo configurable (default $10.000)
                                - Aportes > umbral de revisión quedan en `EN_REVISION` automáticamente
                                - La suma mensual por afiliado no puede superar el tope mensual
                                - Al rechazar o anular, el cupo mensual es liberado
                                - Invariante de parámetros: `montoMinimo` < `umbralRevision` < `topeMensual`
                                - Todos los cambios de estado quedan auditados en `revision_aporte`
                                - Los topes son configurables en caliente vía `POST /api/parametros`
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

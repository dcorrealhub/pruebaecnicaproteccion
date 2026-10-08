package co.proteccion.cis.retob.infrastructure.config;

import co.proteccion.cis.retob.domain.model.Canal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;

/**
 * Parámetros de negocio configurables. Se validan al arrancar: una configuración
 * inválida (tope negativo, zona inexistente, canal desconocido) impide levantar el
 * servicio en vez de fallar en tiempo de ejecución.
 *
 * @param umbralRevisionPorCanal umbral para canales de mayor riesgo; prevalece sobre el
 *                               umbral del afiliado. Ej: {@code aporte.umbral-revision-por-canal.SUCURSAL=3000000}
 */
@Validated
@ConfigurationProperties(prefix = "aporte")
public record AporteProperties(
        @NotNull @Positive BigDecimal topeMensual,
        @NotNull @Positive BigDecimal umbralRevision,
        Map<Canal, @NotNull @Positive BigDecimal> umbralRevisionPorCanal,
        @NotNull ZoneId zonaHoraria
) {
    public AporteProperties {
        umbralRevisionPorCanal = umbralRevisionPorCanal == null ? Map.of() : umbralRevisionPorCanal;
    }
}

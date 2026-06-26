package co.proteccion.cis.retob.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "aporte")
public record AporteProperties(
        BigDecimal topeMensual,
        BigDecimal umbralRevision
) {}
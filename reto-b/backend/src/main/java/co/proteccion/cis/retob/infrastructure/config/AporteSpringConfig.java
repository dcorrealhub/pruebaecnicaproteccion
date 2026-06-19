package co.proteccion.cis.retob.infrastructure.config;

import co.proteccion.cis.retob.application.AporteLimites;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;

@Configuration
class AporteSpringConfig {

    @Bean
    AporteLimites aporteLimites(
            @Value("${aporte.tope-mensual:10000000}") BigDecimal topeMensual,
            @Value("${aporte.umbral-revision:5000000}") BigDecimal umbralRevision) {
        return new AporteLimites(topeMensual, umbralRevision);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}

package co.proteccion.cis.retob.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj del sistema en la zona horaria del negocio. Se inyecta en los casos de uso
 * para que "hoy" no dependa de la zona de la JVM y pueda fijarse en las pruebas.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${aporte.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}

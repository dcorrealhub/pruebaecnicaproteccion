package co.proteccion.cis.retob.infrastructure.config;

import co.proteccion.cis.retob.domain.service.PoliticaAportes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AporteProperties.class)
public class AporteConfig {

    @Bean
    public PoliticaAportes politicaAportes(AporteProperties properties) {
        return new PoliticaAportes(properties.topeMensual(), properties.umbralRevision(),
                properties.umbralRevisionPorCanal());
    }

    /**
     * Reloj del negocio: la fecha del aporte (y por tanto su periodo) se calcula en la
     * zona de Colombia, no en la del servidor. Un aporte a las 8 p.m. del 31 en Bogotá
     * es del mes en curso aunque el contenedor corra en UTC.
     */
    @Bean
    public Clock clock(AporteProperties properties) {
        return Clock.system(properties.zonaHoraria());
    }
}

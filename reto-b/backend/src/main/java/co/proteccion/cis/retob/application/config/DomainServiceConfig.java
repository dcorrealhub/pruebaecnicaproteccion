package co.proteccion.cis.retob.application.config;

import co.proteccion.cis.retob.domain.service.AporteDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public AporteDomainService aporteDomainService() {
        return new AporteDomainService();
    }
}

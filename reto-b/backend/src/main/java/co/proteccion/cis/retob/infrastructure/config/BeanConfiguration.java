package co.proteccion.cis.retob.infrastructure.config;

import co.proteccion.cis.retob.domain.model.aporte.gateway.AporteOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.EventoOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.SaldoOutputPort;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import co.proteccion.cis.retob.domain.usecase.AporteUseCase;
import co.proteccion.cis.retob.domain.usecase.ParametroUseCase;
import co.proteccion.cis.retob.domain.usecase.input.AporteInputPort;
import co.proteccion.cis.retob.domain.usecase.input.ParametroInputPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado manual de los casos de uso de dominio como beans de Spring.
 *
 * <p>Los UseCase se mantienen libres de anotaciones de Spring para poder probarlos sin
 * contexto de aplicación. Cada UseCase nuevo se registra aquí con sus puertos requeridos.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public AporteInputPort aporteInputPort(AporteOutputPort aporteOutputPort,
                                           SaldoOutputPort saldoOutputPort,
                                           EventoOutputPort eventoOutputPort,
                                           ParametroOutputPort parametroOutputPort) {
        return new AporteUseCase(aporteOutputPort, saldoOutputPort, eventoOutputPort, parametroOutputPort);
    }

    @Bean
    public ParametroInputPort parametroInputPort(ParametroOutputPort parametroOutputPort) {
        return new ParametroUseCase(parametroOutputPort);
    }
}

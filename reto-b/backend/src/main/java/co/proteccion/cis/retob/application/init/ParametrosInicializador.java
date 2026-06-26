package co.proteccion.cis.retob.application.init;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ParametrosInicializador {

    private final ParametroRepository parametroRepository;

    @Value("${aporte.monto-minimo:10000}")
    private BigDecimal montoMinimoDefault;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensualDefault;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevisionDefault;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void sembrarParametrosIniciales() {
        if (parametroRepository.findLatest().isEmpty()) {
            parametroRepository.guardarCambio(new ParametrosFondo(
                    null,
                    montoMinimoDefault,
                    topeMensualDefault,
                    umbralRevisionDefault,
                    "SYSTEM",
                    OffsetDateTime.now(),
                    "Carga inicial desde variables de entorno"
            ));
        }
    }
}

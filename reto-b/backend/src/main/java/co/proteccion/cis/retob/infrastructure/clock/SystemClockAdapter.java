package co.proteccion.cis.retob.infrastructure.clock;

import co.proteccion.cis.retob.domain.port.out.ClockPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SystemClockAdapter implements ClockPort {

    @Override
    public LocalDate hoy() {
        return LocalDate.now();
    }
}

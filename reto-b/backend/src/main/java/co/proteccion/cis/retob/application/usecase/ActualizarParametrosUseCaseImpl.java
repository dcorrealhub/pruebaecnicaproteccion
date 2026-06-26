package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ActualizarParametrosUseCaseImpl implements ActualizarParametrosUseCase {

    private final ParametroRepository parametroRepository;

    @Override
    @Transactional
    public ParametrosFondo actualizar(ActualizarParametrosCommand command) {
        // Invariante: montoMinimo < umbralRevision < topeMensual
        if (command.montoMinimo().compareTo(command.umbralRevision()) >= 0) {
            throw new IllegalArgumentException(
                    "El monto mínimo (" + command.montoMinimo() +
                    ") debe ser menor al umbral de revisión (" + command.umbralRevision() + ").");
        }
        if (command.umbralRevision().compareTo(command.topeMensual()) >= 0) {
            throw new IllegalArgumentException(
                    "El umbral de revisión (" + command.umbralRevision() +
                    ") debe ser menor al tope mensual (" + command.topeMensual() + ").");
        }
        ParametrosFondo nuevo = new ParametrosFondo(
                null,
                command.montoMinimo(),
                command.topeMensual(),
                command.umbralRevision(),
                command.modificadoPor(),
                OffsetDateTime.now(),
                command.comentario()
        );
        return parametroRepository.guardarCambio(nuevo);
    }
}

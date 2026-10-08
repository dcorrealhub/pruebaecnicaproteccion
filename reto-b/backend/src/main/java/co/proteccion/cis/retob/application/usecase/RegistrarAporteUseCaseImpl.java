package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Implementación del caso de uso de registro de aportes.
 *
 * <p>Orquesta la operación y gestiona el reintento ante conflictos de concurrencia optimista.
 * La lógica transaccional de un intento vive en {@link RegistrarAporteTransaccion}, de modo
 * que cada reintento abra una transacción nueva (una transacción que falló por bloqueo
 * optimista no puede reutilizarse).</p>
 *
 * <p>Idempotencia: ante una carrera entre dos peticiones con la misma clave, el UNIQUE de
 * base de datos garantiza que solo una inserte; la otra recibe un error de integridad, se
 * recupera releyendo el aporte original y lo devuelve sin duplicar efectos.</p>
 */
@Service
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final int MAX_INTENTOS = 3;

    private final RegistrarAporteTransaccion transaccion;
    private final AporteRepositoryPort aporteRepository;

    public RegistrarAporteUseCaseImpl(RegistrarAporteTransaccion transaccion,
                                      AporteRepositoryPort aporteRepository) {
        this.transaccion = transaccion;
        this.aporteRepository = aporteRepository;
    }

    @Override
    public Aporte registrar(RegistrarAporteCommand command) {
        validarFecha(command.fecha());

        OptimisticLockingFailureException ultimoConflicto = null;

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                return transaccion.ejecutar(command);
            } catch (DataIntegrityViolationException e) {
                // Carrera idempotente: otra petición con la misma clave ya insertó.
                // Recuperar el original y devolverlo (sin duplicar).
                return aporteRepository.findByIdempotenciaKey(command.idempotenciaKey())
                        .orElseThrow(() -> e);
            } catch (OptimisticLockingFailureException e) {
                // Conflicto sobre el saldo mensual: reintentar con transacción nueva.
                ultimoConflicto = e;
            }
        }

        throw new ConflictoConcurrenciaException(
                "No se pudo registrar el aporte por alta concurrencia. Reintente la operación.",
                ultimoConflicto);
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha del aporte es obligatoria.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha del aporte no puede ser futura.");
        }
    }
}

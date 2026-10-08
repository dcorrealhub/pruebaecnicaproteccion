package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.enums.Canal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto de entrada (caso de uso): registrar un aporte voluntario.
 */
public interface RegistrarAporteUseCase {

    /**
     * Registra un aporte. La operación es idempotente: un reintento con la misma
     * {@code idempotenciaKey} y el mismo contenido no duplica el aporte y retorna el original.
     *
     * @param command datos del aporte a registrar
     * @return {@link Creado} si el aporte se registró, {@link Repetido} si ya existía
     * @throws co.proteccion.cis.retob.domain.exception.AporteRechazadoException si se viola una regla de negocio
     * @throws co.proteccion.cis.retob.domain.exception.IdempotenciaConflictoException si la clave ya se usó con otro contenido
     * @throws co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException si otra operación concurrente modificó los mismos datos
     */
    RegistroAporte registrar(RegistrarAporteCommand command);

    record RegistrarAporteCommand(
            String afiliadoId,
            BigDecimal monto,
            LocalDate fecha,
            Canal canal,
            String idempotenciaKey
    ) {}

    /**
     * Resultado del registro: distingue un aporte nuevo de un reintento idempotente.
     */
    sealed interface RegistroAporte permits Creado, Repetido {
        Aporte aporte();
    }

    record Creado(Aporte aporte) implements RegistroAporte {}

    record Repetido(Aporte aporte) implements RegistroAporte {}
}

package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto de entrada (caso de uso): registrar un aporte voluntario.
 * Separación comando / consulta: este método ejecuta un comando y no retorna proyección.
 */
public interface RegistrarAporteUseCase {

    /**
     * Registra un aporte y retorna el identificador generado.
     * La operación es idempotente: reintentos con la misma {@code idempotenciaKey}
     * no duplican el aporte.
     *
     * @param command datos del aporte a registrar
     * @return el aporte persistido
     * @throws IllegalArgumentException si las reglas de negocio son violadas
     */
    Aporte registrar(RegistrarAporteCommand command);

    /**
     * @param fecha fecha del aporte; si es {@code null} se asume la fecha actual del servidor.
     *              El periodo (YYYY-MM) se deriva de ella.
     */
    record RegistrarAporteCommand(
            String afiliadoId,
            BigDecimal monto,
            LocalDate fecha,
            String canal,
            String idempotenciaKey
    ) {}
}

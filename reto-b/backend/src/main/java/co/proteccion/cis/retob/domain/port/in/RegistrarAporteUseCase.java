package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.math.BigDecimal;

/**
 * Puerto de entrada (caso de uso): registrar un aporte voluntario.
 * Separación comando / consulta: el comando solo retorna lo necesario para
 * confirmar la operación (el aporte persistido y si fue creado en esta llamada).
 */
public interface RegistrarAporteUseCase {

    /**
     * Registra un aporte. La fecha la asigna el servidor.
     * La operación es idempotente: reintentos con la misma {@code idempotenciaKey}
     * y el mismo contenido retornan el aporte original sin duplicarlo.
     *
     * @param command datos del aporte a registrar
     * @return el aporte persistido y si fue creado en esta invocación
     * @throws co.proteccion.cis.retob.domain.exception.ReglaNegocioException        si se viola una regla de negocio
     * @throws co.proteccion.cis.retob.domain.exception.SolicitudInvalidaException   si el canal no es válido
     * @throws co.proteccion.cis.retob.domain.exception.ConflictoIdempotenciaException si la clave ya se usó con otro contenido
     * @throws co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException si hubo una escritura concurrente (reintentable)
     */
    ResultadoRegistro registrar(RegistrarAporteCommand command);

    record RegistrarAporteCommand(
            String afiliadoId,
            BigDecimal monto,
            String canal,
            String idempotenciaKey
    ) {}

    /**
     * @param creado {@code false} cuando la respuesta corresponde a un reintento idempotente
     */
    record ResultadoRegistro(Aporte aporte, boolean creado) {}
}

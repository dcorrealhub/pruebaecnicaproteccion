package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maneja la inicialización concurrente del saldo mensual.
 *
 * Si dos hilos detectan simultáneamente que no existe el saldo y ambos intentan
 * insertarlo, el segundo recibirá DataIntegrityViolationException (UNIQUE constraint).
 * Este componente captura ese conflicto en REQUIRES_NEW (savepoint propio) y relee
 * el registro que ya insertó el primer hilo, evitando que la transacción principal
 * quede marcada como rollback-only.
 */
@Component
@RequiredArgsConstructor
public class SaldoInicializadorAdapter {

    private final SaldoRepositoryPort saldoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SaldoMensual obtenerOInicializar(String afiliadoId, String mes) {
        return saldoRepository.findByAfiliadoIdAndMes(afiliadoId, mes)
                .orElseGet(() -> {
                    try {
                        return saldoRepository.inicializar(afiliadoId, mes);
                    } catch (DataIntegrityViolationException ex) {
                        // Otro hilo ganó la carrera — releer el registro ya insertado
                        return saldoRepository.findByAfiliadoIdAndMes(afiliadoId, mes)
                                .orElseThrow(() -> new IllegalStateException(
                                        "No se pudo obtener ni crear el saldo mensual para "
                                        + afiliadoId + "/" + mes, ex));
                    }
                });
    }
}

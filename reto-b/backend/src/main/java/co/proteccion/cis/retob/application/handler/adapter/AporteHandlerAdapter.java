package co.proteccion.cis.retob.application.handler.adapter;

import co.proteccion.cis.retob.application.handler.AporteHandler;
import co.proteccion.cis.retob.domain.constant.AporteMessages;
import co.proteccion.cis.retob.domain.exception.DomainBusinessRuleException;
import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.aporte.ConsultaConsolidado;
import co.proteccion.cis.retob.domain.usecase.input.AporteInputPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Facade transaccional del caso de uso de aportes.
 *
 * <p>Orquesta lo que el dominio puro no debe conocer: cada operación de escritura corre en
 * su propia transacción y se reintenta ante un conflicto de bloqueo optimista sobre el saldo.
 * La carrera de idempotencia (dos inserts con la misma clave a la vez) se resuelve reintentando:
 * el reintento encuentra el aporte ganador en la verificación previa del caso de uso y lo devuelve.
 */
@Service
public class AporteHandlerAdapter implements AporteHandler {

    private static final int MAX_REINTENTOS = 3;

    private final AporteInputPort aporteInputPort;
    private final TransactionTemplate tx;

    public AporteHandlerAdapter(AporteInputPort aporteInputPort,
                                PlatformTransactionManager transactionManager) {
        this.aporteInputPort = aporteInputPort;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public Aporte registrar(Aporte aporte) {
        return ejecutarConReintentos(() -> aporteInputPort.registrar(aporte));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultaConsolidado consulta) {
        return aporteInputPort.consultar(consulta);
    }

    @Override
    public Aporte aprobar(Long aporteId) {
        return ejecutarConReintentos(() -> aporteInputPort.aprobar(aporteId));
    }

    @Override
    public Aporte rechazar(Long aporteId) {
        return ejecutarConReintentos(() -> aporteInputPort.rechazar(aporteId));
    }

    private Aporte ejecutarConReintentos(Supplier<Aporte> operacion) {
        for (int intento = 0; intento < MAX_REINTENTOS; intento++) {
            try {
                return tx.execute(status -> operacion.get());
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException conflicto) {
                // Saldo modificado en paralelo o carrera de idempotencia: releer y reintentar.
            }
        }
        throw new DomainBusinessRuleException(AporteMessages.CONCURRENCIA);
    }
}

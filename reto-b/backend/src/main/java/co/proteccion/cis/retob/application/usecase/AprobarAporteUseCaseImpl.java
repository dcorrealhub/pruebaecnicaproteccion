package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.AporteNoEncontradoException;
import co.proteccion.cis.retob.domain.model.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.AprobarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Resolución de aportes marcados para revisión.
 *
 * <p>Bajo el modelo de <b>reserva</b>, un aporte pendiente ya descontó cupo del tope al
 * registrarse. Por eso:
 * <ul>
 *   <li><b>Aprobar</b> solo cambia el estado; el saldo no se modifica (ya estaba reservado),
 *       por lo que la aprobación nunca puede superar el tope.</li>
 *   <li><b>Rechazar</b> libera la reserva: decrementa el saldo del mes por el monto del aporte.</li>
 * </ul>
 */
@Service
public class AprobarAporteUseCaseImpl implements AprobarAporteUseCase {

    private static final int MAX_REINTENTOS = 3;

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final EventoAportePort eventoPort;
    private final TransactionTemplate tx;

    public AprobarAporteUseCaseImpl(AporteRepositoryPort aporteRepository,
                                    SaldoRepositoryPort saldoRepository,
                                    EventoAportePort eventoPort,
                                    PlatformTransactionManager transactionManager) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
        this.eventoPort = eventoPort;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public Aporte aprobar(Long aporteId) {
        Aporte pendiente = cargar(aporteId);
        Aporte aprobado = pendiente.aprobar(); // valida que esté PENDIENTE_REVISION
        return tx.execute(status -> {
            Aporte guardado = aporteRepository.guardar(aprobado);
            eventoPort.registrar(guardado.getId(), EventoAportePort.Tipo.APORTE_APROBADO);
            return guardado;
        });
    }

    @Override
    public Aporte rechazar(Long aporteId) {
        Aporte pendiente = cargar(aporteId);
        Aporte rechazado = pendiente.rechazar(); // valida que esté PENDIENTE_REVISION

        for (int intento = 0; intento < MAX_REINTENTOS; intento++) {
            try {
                return tx.execute(status -> {
                    // Libera la reserva del tope que el aporte tomó al registrarse.
                    saldoRepository.findByAfiliadoIdAndMes(rechazado.getAfiliadoId(), rechazado.getPeriodo())
                            .ifPresent(saldo -> saldoRepository.guardar(
                                    saldo.conTotal(saldo.getTotal().subtract(rechazado.getMonto()))));
                    Aporte guardado = aporteRepository.guardar(rechazado);
                    eventoPort.registrar(guardado.getId(), EventoAportePort.Tipo.APORTE_RECHAZADO);
                    return guardado;
                });
            } catch (OptimisticLockingFailureException conflicto) {
                // saldo modificado en paralelo: releer y reintentar
            }
        }
        throw new ReglaNegocioException(
                "No se pudo rechazar el aporte por alta concurrencia sobre el saldo mensual; reintente.");
    }

    private Aporte cargar(Long aporteId) {
        return aporteRepository.findById(aporteId)
                .orElseThrow(() -> new AporteNoEncontradoException(aporteId));
    }
}

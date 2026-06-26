package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.domain.port.out.ParametroAportePort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso de registro de aportes.
 *
 * <p>Reglas implementadas:
 * <ol>
 *   <li><b>Idempotencia</b>: dos solicitudes con la misma {@code idempotenciaKey}
 *       producen un único aporte; los reintentos devuelven el ya persistido.</li>
 *   <li><b>Monto positivo</b>: validado en el dominio ({@link Aporte#nuevo}).</li>
 *   <li><b>Umbral de revisión</b>: si el monto lo supera, el aporte queda
 *       {@code PENDIENTE_REVISION}.</li>
 *   <li><b>Tope mensual (reserva)</b>: <b>todo</b> aporte —aprobado o pendiente—
 *       reserva cupo contra el tope. El saldo mensual acumula aprobados + pendientes,
 *       de modo que un pendiente, al aprobarse, nunca puede superar el tope. Si el
 *       acumulado resultante lo excede, el aporte se rechaza con {@link ReglaNegocioException}.</li>
 *   <li><b>Concurrencia</b>: el saldo se actualiza con bloqueo optimista; un
 *       conflicto se reintenta releyendo el saldo fresco.</li>
 * </ol>
 *
 * <p>Se usa {@link TransactionTemplate} (en vez de {@code @Transactional}) para poder
 * reintentar cada intento en su propia transacción ante un conflicto de concurrencia.
 */
@Service
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final int MAX_REINTENTOS = 3;

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final ParametroAportePort parametroPort;
    private final EventoAportePort eventoPort;
    private final TransactionTemplate tx;

    public RegistrarAporteUseCaseImpl(AporteRepositoryPort aporteRepository,
                                      SaldoRepositoryPort saldoRepository,
                                      ParametroAportePort parametroPort,
                                      EventoAportePort eventoPort,
                                      PlatformTransactionManager transactionManager) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
        this.parametroPort = parametroPort;
        this.eventoPort = eventoPort;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public Aporte registrar(RegistrarAporteCommand command) {
        // 1. Idempotencia: si ya existe, devolver sin reprocesar.
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        LocalDate fecha = command.fecha() != null ? command.fecha() : LocalDate.now();
        ParametrosAporte params = parametroPort.forAfiliado(command.afiliadoId());

        // 2. El monto se valida en el dominio. El umbral decide el estado inicial,
        //    pero ambos estados reservan cupo contra el tope.
        boolean superaUmbral = params.superaUmbral(command.monto());
        EstadoAporte estado = superaUmbral ? EstadoAporte.PENDIENTE_REVISION : EstadoAporte.APROBADO;
        Aporte aporte = Aporte.nuevo(
                command.afiliadoId(), command.monto(), fecha, command.canal(), estado, command.idempotenciaKey());

        return persistirReservandoTope(aporte, params, superaUmbral);
    }

    /**
     * Valida el tope contra el saldo del mes (aprobados + pendientes) e incrementa el
     * acumulado. Aplica tanto a aportes aprobados como pendientes: el pendiente reserva
     * cupo para que, al aprobarse, no pueda superar el tope.
     */
    private Aporte persistirReservandoTope(Aporte aporte, ParametrosAporte params, boolean marcarRevision) {
        for (int intento = 0; intento < MAX_REINTENTOS; intento++) {
            try {
                return ejecutarManejandoCarrera(aporte.getIdempotenciaKey(), () -> tx.execute(status -> {
                    SaldoMensual saldo = saldoRepository
                            .findByAfiliadoIdAndMes(aporte.getAfiliadoId(), aporte.getPeriodo())
                            .orElseGet(() -> saldoRepository.inicializar(aporte.getAfiliadoId(), aporte.getPeriodo()));

                    BigDecimal nuevoTotal = saldo.calcularNuevoTotal(aporte.getMonto());
                    if (params.superaTope(nuevoTotal)) {
                        throw new ReglaNegocioException(
                                "El aporte supera el tope mensual del afiliado (tope: %s, acumulado resultante: %s)"
                                        .formatted(params.topeMensual().toPlainString(), nuevoTotal.toPlainString()));
                    }

                    saldoRepository.guardar(saldo.conTotal(nuevoTotal));
                    Aporte guardado = aporteRepository.guardar(aporte);
                    eventoPort.registrar(guardado.getId(), EventoAportePort.Tipo.APORTE_REGISTRADO);
                    if (marcarRevision) {
                        eventoPort.registrar(guardado.getId(), EventoAportePort.Tipo.APORTE_MARCADO_REVISION);
                    }
                    return guardado;
                }));
            } catch (OptimisticLockingFailureException conflicto) {
                // saldo modificado en paralelo: releer y reintentar
            }
        }
        throw new ReglaNegocioException(
                "No se pudo registrar el aporte por alta concurrencia sobre el saldo mensual; reintente.");
    }

    /**
     * Envuelve la persistencia para resolver la carrera de idempotencia: si dos
     * solicitudes con la misma clave insertan a la vez, la perdedora recibe una
     * violación de unicidad y aquí devolvemos el aporte ganador ya persistido.
     */
    private Aporte ejecutarManejandoCarrera(String idempotenciaKey, java.util.function.Supplier<Aporte> accion) {
        try {
            return accion.get();
        } catch (DataIntegrityViolationException e) {
            return aporteRepository.findByIdempotenciaKey(idempotenciaKey)
                    .orElseThrow(() -> e);
        }
    }
}

package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.PoliticaAportes;
import co.proteccion.cis.retob.domain.model.ResultadoEvaluacion;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.domain.port.out.ParametrosAportePort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Colaborador transaccional del registro de aportes: ejecuta un único intento dentro de
 * una transacción propia. El bucle de reintento ante conflicto de concurrencia vive en
 * {@link RegistrarAporteUseCaseImpl}, de modo que cada reintento abra una transacción nueva.
 */
@Component
public class RegistrarAporteTransaccion {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final ParametrosAportePort parametrosPort;
    private final EventoAportePort eventoPort;

    public RegistrarAporteTransaccion(AporteRepositoryPort aporteRepository,
                                      SaldoRepositoryPort saldoRepository,
                                      ParametrosAportePort parametrosPort,
                                      EventoAportePort eventoPort) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
        this.parametrosPort = parametrosPort;
        this.eventoPort = eventoPort;
    }

    /**
     * Ejecuta un intento de registro. Puede lanzar una excepción de bloqueo optimista
     * (p. ej. {@link org.springframework.orm.ObjectOptimisticLockingFailureException})
     * que el llamador captura para reintentar.
     */
    @Transactional
    public Aporte ejecutar(RegistrarAporteCommand command) {
        // 1. Idempotencia: si ya existe un aporte con la misma clave, devolver el original.
        Optional<Aporte> existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        String mes = command.fecha().format(PERIODO_FMT);

        // 2. Resolver parámetros aplicables (global hoy; por afiliado/mes en el futuro).
        ParametrosAporte parametros = parametrosPort.resolver(command.afiliadoId(), mes);

        // 3. Cargar o inicializar el saldo del mes.
        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), mes)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), mes));

        // 4. Evaluar reglas de negocio en el dominio (monto > 0, tope, umbral de revisión).
        BigDecimal acumulado = saldo.getTotal();
        ResultadoEvaluacion evaluacion = PoliticaAportes.evaluar(
                command.monto(), command.canal(), acumulado, parametros);

        // 5. Construir y persistir el aporte (el dominio normaliza monto y deriva periodo).
        Aporte aporte = Aporte.nuevo(
                command.afiliadoId(),
                command.monto(),
                command.fecha(),
                command.canal(),
                evaluacion.marcadaRevision(),
                command.idempotenciaKey());
        Aporte persistido = aporteRepository.guardar(aporte);

        // 6. Actualizar el saldo mensual (control de concurrencia optimista en el adaptador).
        SaldoMensual saldoActualizado = saldo.conTotal(saldo.calcularNuevoTotal(persistido.getMonto()));
        saldoRepository.guardar(saldoActualizado);

        // 7. Trazabilidad: evento de aporte registrado.
        eventoPort.registrarCreado(persistido);

        return persistido;
    }
}

package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteRechazadoException;
import co.proteccion.cis.retob.domain.exception.IdempotenciaConflictoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EventoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.enums.Canal;
import co.proteccion.cis.retob.domain.model.enums.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * Implementación del caso de uso de registro de aportes.
 *
 * <p>Reglas: monto positivo, fecha no futura, tope mensual por afiliado (mes calendario de la
 * fecha del aporte) y marca de revisión cuando el monto supera el umbral de su canal.
 * La idempotencia se apoya en la {@code idempotenciaKey} y la concurrencia sobre el saldo
 * mensual en el control optimista del puerto de saldos.
 */
@Service
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final EventoAporteRepositoryPort eventoAporteRepository;
    private final Clock clock;
    private final BigDecimal topeMensual;
    private final BigDecimal umbralRevisionDefault;
    private final Map<Canal, BigDecimal> umbralRevisionPorCanal;

    public RegistrarAporteUseCaseImpl(
            AporteRepositoryPort aporteRepository,
            SaldoRepositoryPort saldoRepository,
            EventoAporteRepositoryPort eventoAporteRepository,
            Clock clock,
            @Value("${aporte.tope-mensual}") BigDecimal topeMensual,
            @Value("${aporte.umbral-revision}") BigDecimal umbralRevisionDefault,
            @Value("#{${aporte.umbral-revision.por-canal}}") Map<Canal, BigDecimal> umbralRevisionPorCanal) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
        this.eventoAporteRepository = eventoAporteRepository;
        this.clock = clock;
        this.topeMensual = topeMensual;
        this.umbralRevisionDefault = umbralRevisionDefault;
        this.umbralRevisionPorCanal = Map.copyOf(umbralRevisionPorCanal);
    }

    @Override
    @Transactional
    public RegistroAporte registrar(RegistrarAporteCommand command) {
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return reintento(existente.get(), command);
        }

        validarMonto(command.monto());
        validarFecha(command.fecha());

        String periodo = YearMonth.from(command.fecha()).toString();
        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        validarTope(saldo, nuevoTotal, periodo);

        saldoRepository.guardar(saldo.conTotal(nuevoTotal));
        Aporte aporte = aporteRepository.guardar(new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                command.fecha(),
                command.canal(),
                periodo,
                superaUmbralRevision(command.monto(), command.canal()),
                command.idempotenciaKey()
        ));
        eventoAporteRepository.guardar(
                new EventoAporte(null, aporte.getId(), TipoEventoAporte.APORTE_REGISTRADO, null));

        return new Creado(aporte);
    }

    private RegistroAporte reintento(Aporte existente, RegistrarAporteCommand command) {
        if (!mismoContenido(existente, command)) {
            throw new IdempotenciaConflictoException(command.idempotenciaKey());
        }
        return new Repetido(existente);
    }

    private boolean mismoContenido(Aporte aporte, RegistrarAporteCommand command) {
        return aporte.getAfiliadoId().equals(command.afiliadoId())
                && aporte.getMonto().compareTo(command.monto()) == 0
                && aporte.getFecha().equals(command.fecha())
                && aporte.getCanal() == command.canal();
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new AporteRechazadoException("MONTO_NO_POSITIVO", "El monto del aporte debe ser mayor a cero");
        }
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new AporteRechazadoException("FECHA_FUTURA", "La fecha del aporte no puede ser futura");
        }
    }

    private void validarTope(SaldoMensual saldo, BigDecimal nuevoTotal, String periodo) {
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            BigDecimal disponible = topeMensual.subtract(saldo.getTotal()).max(BigDecimal.ZERO);
            throw new AporteRechazadoException("TOPE_MENSUAL_EXCEDIDO",
                    "El aporte supera el tope mensual de " + topeMensual.toPlainString()
                            + " para el periodo " + periodo
                            + ". Disponible: " + disponible.toPlainString());
        }
    }

    private boolean superaUmbralRevision(BigDecimal monto, Canal canal) {
        BigDecimal umbral = umbralRevisionPorCanal.getOrDefault(canal, umbralRevisionDefault);
        return monto.compareTo(umbral) > 0;
    }
}

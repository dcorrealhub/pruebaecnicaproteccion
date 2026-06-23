package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.application.config.AporteProperties;
import co.proteccion.cis.retob.application.service.IdempotenciaAporteService;
import co.proteccion.cis.retob.domain.exception.ConcurrenciaSaldoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.CanalAporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.ClockPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.domain.service.AporteDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final int MAX_REINTENTOS_CONCURRENCIA = 3;

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final EventoAporteRepositoryPort eventoRepository;
    private final ClockPort clock;
    private final AporteDomainService domainService;
    private final AporteProperties properties;
    private final IdempotenciaAporteService idempotenciaService;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        String key = command.idempotenciaKey();

        var existente = idempotenciaService.resolverAporteExistente(key);
        if (existente.isPresent()) {
            return existente.get();
        }

        if (!idempotenciaService.intentarClaim(key)) {
            return idempotenciaService.resolverTrasConflicto(key);
        }

        try {
            Aporte aporte = registrarNuevoAporte(command);
            idempotenciaService.completar(key, aporte.getId());
            return aporte;
        } catch (RuntimeException ex) {
            idempotenciaService.liberarClaim(key);
            throw ex;
        }
    }

    private Aporte registrarNuevoAporte(RegistrarAporteCommand command) {
        domainService.validarMontoPositivo(command.monto());

        LocalDate fecha = clock.hoy();
        String periodo = domainService.calcularPeriodo(fecha);
        CanalAporte canal = domainService.parseCanal(command.canal());
        EstadoAporte estado = domainService.determinarEstado(command.monto(), properties.umbralRevision());

        for (int intento = 0; intento < MAX_REINTENTOS_CONCURRENCIA; intento++) {
            try {
                return ejecutarRegistro(command, fecha, periodo, canal, estado);
            } catch (ConcurrenciaSaldoException ex) {
                if (intento == MAX_REINTENTOS_CONCURRENCIA - 1) {
                    throw ex;
                }
            }
        }

        throw new IllegalStateException("Registro de aporte interrumpido tras agotar reintentos");
    }

    private Aporte ejecutarRegistro(RegistrarAporteCommand command,
                                    LocalDate fecha,
                                    String periodo,
                                    CanalAporte canal,
                                    EstadoAporte estado) {
        SaldoMensual saldo = obtenerSaldo(command.afiliadoId(), periodo);
        domainService.validarTopeMensual(saldo, command.monto(), properties.topeMensual());

        Aporte aporte = Aporte.nuevo(
                command.afiliadoId(),
                command.monto(),
                fecha,
                canal,
                periodo,
                estado,
                command.idempotenciaKey()
        );

        Aporte persistido = aporteRepository.guardar(aporte);

        SaldoMensual saldoActualizado = saldo.conTotal(saldo.calcularNuevoTotal(command.monto()));
        saldoRepository.guardar(saldoActualizado);

        eventoRepository.registrar(persistido.getId(), TipoEventoAporte.APORTE_REGISTRADO);

        return persistido;
    }

    private SaldoMensual obtenerSaldo(String afiliadoId, String periodo) {
        return saldoRepository.findByAfiliadoIdAndMes(afiliadoId, periodo)
                .orElseGet(() -> saldoRepository.inicializar(afiliadoId, periodo));
    }
}

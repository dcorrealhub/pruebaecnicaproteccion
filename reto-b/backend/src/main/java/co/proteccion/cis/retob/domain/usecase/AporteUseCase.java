package co.proteccion.cis.retob.domain.usecase;

import co.proteccion.cis.retob.domain.constant.AporteMessages;
import co.proteccion.cis.retob.domain.exception.DomainBusinessRuleException;
import co.proteccion.cis.retob.domain.exception.DomainNotFoundException;
import co.proteccion.cis.retob.domain.exception.DomainValidationException;
import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.aporte.ConsultaConsolidado;
import co.proteccion.cis.retob.domain.model.aporte.EstadoAporte;
import co.proteccion.cis.retob.domain.model.aporte.SaldoMensual;
import co.proteccion.cis.retob.domain.model.aporte.gateway.AporteOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.EventoOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.SaldoOutputPort;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import co.proteccion.cis.retob.domain.usecase.input.AporteInputPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reglas de negocio de aportes voluntarios. Clase de dominio pura (sin Spring):
 * la orquestación transaccional y el reintento por concurrencia viven en el handler.
 *
 * <ul>
 *   <li><b>Idempotencia</b>: la misma clave devuelve el aporte existente.</li>
 *   <li><b>Monto positivo</b> y datos obligatorios.</li>
 *   <li><b>Umbral</b>: supera el umbral ⇒ {@code PENDIENTE_REVISION}.</li>
 *   <li><b>Tope (reserva)</b>: todo aporte reserva cupo; si el acumulado excede el tope se rechaza.</li>
 *   <li><b>Aprobar</b> no toca el saldo (ya reservado); <b>rechazar</b> libera la reserva.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class AporteUseCase implements AporteInputPort {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteOutputPort aporteOutputPort;
    private final SaldoOutputPort saldoOutputPort;
    private final EventoOutputPort eventoOutputPort;
    private final ParametroOutputPort parametroOutputPort;

    @Override
    public Aporte registrar(Aporte aporte) {
        // 1. Idempotencia: si ya existe, se devuelve sin reprocesar.
        var existente = aporteOutputPort.findByIdempotenciaKey(aporte.getIdempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        validarRegistro(aporte);

        LocalDate fecha = aporte.getFecha() != null ? aporte.getFecha() : LocalDate.now();
        String periodo = fecha.format(PERIODO_FMT);
        ParametrosAporte params = parametroOutputPort.forAfiliado(aporte.getAfiliadoId());

        boolean superaUmbral = aporte.getMonto().compareTo(params.umbralRevision()) > 0;
        EstadoAporte estado = superaUmbral ? EstadoAporte.PENDIENTE_REVISION : EstadoAporte.APROBADO;

        // 2. Reserva de cupo: todo aporte (aprobado o pendiente) valida e incrementa el saldo.
        SaldoMensual saldo = saldoOutputPort.findByAfiliadoIdAndMes(aporte.getAfiliadoId(), periodo)
                .orElseGet(() -> saldoOutputPort.inicializar(aporte.getAfiliadoId(), periodo));
        BigDecimal nuevoTotal = saldo.getTotal().add(aporte.getMonto());
        if (nuevoTotal.compareTo(params.topeMensual()) > 0) {
            throw new DomainBusinessRuleException(AporteMessages.TOPE_EXCEDIDO_FMT.formatted(
                    params.topeMensual().toPlainString(), nuevoTotal.toPlainString()));
        }
        saldoOutputPort.save(saldo.toBuilder().total(nuevoTotal).build());

        Aporte aGuardar = aporte.toBuilder()
                .id(null)
                .fecha(fecha)
                .periodo(periodo)
                .estado(estado)
                .build();
        Aporte guardado = aporteOutputPort.save(aGuardar);

        eventoOutputPort.registrar(guardado.getId(), EventoOutputPort.Tipo.APORTE_REGISTRADO);
        if (superaUmbral) {
            eventoOutputPort.registrar(guardado.getId(), EventoOutputPort.Tipo.APORTE_MARCADO_REVISION);
        }
        return guardado;
    }

    @Override
    public ConsolidadoAportes consultar(ConsultaConsolidado consulta) {
        List<Aporte> detalle = aporteOutputPort.findByAfiliadoIdAndPeriodoBetween(
                consulta.afiliadoId(), consulta.periodoDesde(), consulta.periodoHasta());
        return new ConsolidadoAportes(
                consulta.afiliadoId(),
                consulta.periodoDesde(),
                consulta.periodoHasta(),
                sumarPorEstado(detalle, EstadoAporte.APROBADO),
                sumarPorEstado(detalle, EstadoAporte.PENDIENTE_REVISION),
                detalle);
    }

    @Override
    public Aporte aprobar(Long aporteId) {
        Aporte pendiente = cargarPendiente(aporteId, AporteMessages.SOLO_PENDIENTE_APROBAR);
        // El cupo ya fue reservado al registrarse: aprobar no modifica el saldo.
        Aporte aprobado = aporteOutputPort.save(pendiente.toBuilder().estado(EstadoAporte.APROBADO).build());
        eventoOutputPort.registrar(aprobado.getId(), EventoOutputPort.Tipo.APORTE_APROBADO);
        return aprobado;
    }

    @Override
    public Aporte rechazar(Long aporteId) {
        Aporte pendiente = cargarPendiente(aporteId, AporteMessages.SOLO_PENDIENTE_RECHAZAR);
        // Rechazar libera la reserva que el aporte tomó del tope.
        saldoOutputPort.findByAfiliadoIdAndMes(pendiente.getAfiliadoId(), pendiente.getPeriodo())
                .ifPresent(saldo -> saldoOutputPort.save(
                        saldo.toBuilder().total(saldo.getTotal().subtract(pendiente.getMonto())).build()));
        Aporte rechazado = aporteOutputPort.save(pendiente.toBuilder().estado(EstadoAporte.RECHAZADO).build());
        eventoOutputPort.registrar(rechazado.getId(), EventoOutputPort.Tipo.APORTE_RECHAZADO);
        return rechazado;
    }

    private Aporte cargarPendiente(Long aporteId, String mensajeTransicion) {
        Aporte aporte = aporteOutputPort.findById(aporteId)
                .orElseThrow(() -> new DomainNotFoundException(AporteMessages.NOT_FOUND));
        if (aporte.getEstado() != EstadoAporte.PENDIENTE_REVISION) {
            throw new DomainBusinessRuleException(mensajeTransicion);
        }
        return aporte;
    }

    private void validarRegistro(Aporte aporte) {
        if (isBlank(aporte.getAfiliadoId())) {
            throw new DomainValidationException(AporteMessages.AFILIADO_REQUERIDO);
        }
        if (isBlank(aporte.getCanal())) {
            throw new DomainValidationException(AporteMessages.CANAL_REQUERIDO);
        }
        if (isBlank(aporte.getIdempotenciaKey())) {
            throw new DomainValidationException(AporteMessages.IDEMPOTENCIA_REQUERIDA);
        }
        if (aporte.getMonto() == null || aporte.getMonto().signum() <= 0) {
            throw new DomainValidationException(AporteMessages.MONTO_POSITIVO);
        }
    }

    private BigDecimal sumarPorEstado(List<Aporte> aportes, EstadoAporte estado) {
        return aportes.stream()
                .filter(a -> a.getEstado() == estado)
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ConflictoIdempotenciaException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.ParametrosAfiliadoRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.domain.service.PoliticaAportes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Registro de aportes voluntarios.
 *
 * <p>Concurrencia: el acumulado mensual vive en {@code saldo_mensual} con versión optimista.
 * Dos aportes simultáneos del mismo afiliado y mes compiten por la misma fila; el perdedor
 * recibe {@code ConflictoConcurrenciaException} y toda la transacción (saldo, aporte, evento)
 * se revierte. Como el cliente reintenta con la misma clave de idempotencia, el reintento
 * es seguro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final EventoAporteRepositoryPort eventoRepository;
    private final ParametrosAfiliadoRepositoryPort parametrosRepository;
    private final PoliticaAportes politica;
    private final Clock clock;

    @Override
    @Transactional
    public ResultadoRegistro registrar(RegistrarAporteCommand command) {
        Canal canal = Canal.desde(command.canal());
        politica.validarMonto(command.monto());

        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            Aporte original = existente.get();
            if (!original.mismoContenido(command.afiliadoId(), command.monto(), canal)) {
                throw new ConflictoIdempotenciaException();
            }
            log.info("Reintento idempotente: aporteId={}", original.getId());
            return new ResultadoRegistro(original, false);
        }

        BigDecimal monto = command.monto().setScale(2, RoundingMode.UNNECESSARY);
        LocalDate fecha = LocalDate.now(clock);
        String periodo = YearMonth.from(fecha).toString();

        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        ParametrosAfiliado parametros = parametrosRepository.findByAfiliadoId(command.afiliadoId())
                .orElse(ParametrosAfiliado.POR_DEFECTO);

        politica.validarTopeMensual(saldo.getTotal(), monto, politica.topeAplicable(parametros), periodo);
        saldoRepository.guardar(saldo.conTotal(saldo.calcularNuevoTotal(monto)));

        Aporte nuevo = aporteRepository.guardar(new Aporte(
                null,
                command.afiliadoId(),
                monto,
                fecha,
                canal,
                periodo,
                politica.requiereRevision(monto, politica.umbralAplicable(parametros, canal)),
                command.idempotenciaKey()));

        eventoRepository.registrar(nuevo.getId(), TipoEventoAporte.APORTE_REGISTRADO);

        log.info("Aporte registrado: aporteId={} periodo={} marcadaRevision={}",
                nuevo.getId(), periodo, nuevo.isMarcadaRevision());
        return new ResultadoRegistro(nuevo, true);
    }
}

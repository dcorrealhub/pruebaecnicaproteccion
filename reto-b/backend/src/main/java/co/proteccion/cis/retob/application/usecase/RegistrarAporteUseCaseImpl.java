package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        log.info("Registrando aporte: afiliadoId={}, monto={}, canal={}, idempotenciaKey={}",
                command.afiliadoId(), command.monto(), command.canal(), command.idempotenciaKey());

        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            log.info("Aporte ya existente con idempotenciaKey={}, retornando existente id={}",
                    command.idempotenciaKey(), existente.get().getId());
            return existente.get();
        }

        var monto = command.monto();
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Monto inválido: {}", monto);
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        var saldoOpt = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo);
        if (saldoOpt.isEmpty()) {
            log.info("Saldo no encontrado para afiliadoId={} mes={}, inicializando", command.afiliadoId(), periodo);
        }
        var saldo = saldoOpt.orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        var nuevoTotal = saldo.calcularNuevoTotal(monto);
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            log.warn("Tope mensual excedido: afiliadoId={}, saldoActual={}, monto={}, tope={}",
                    command.afiliadoId(), saldo.getTotal(), monto, topeMensual);
            throw new IllegalArgumentException("El monto supera el tope mensual permitido");
        }

        saldoRepository.guardar(saldo.conTotal(nuevoTotal));
        log.info("Saldo actualizado: afiliadoId={}, mes={}, nuevoTotal={}",
                command.afiliadoId(), periodo, nuevoTotal);

        var marcadaRevision = monto.compareTo(umbralRevision) > 0;
        if (marcadaRevision) {
            log.info("Aporte marcado para revisión: monto={} supera umbral={}", monto, umbralRevision);
        }

        var aporte = new Aporte(
                null,
                command.afiliadoId(),
                monto,
                LocalDate.now(),
                command.canal(),
                periodo,
                marcadaRevision,
                command.idempotenciaKey()
        );

        var guardado = aporteRepository.guardar(aporte);
        log.info("Aporte registrado exitosamente: id={}, afiliadoId={}, monto={}, periodo={}",
                guardado.getId(), guardado.getAfiliadoId(), guardado.getMonto(), guardado.getPeriodo());
        return guardado;
    }
}

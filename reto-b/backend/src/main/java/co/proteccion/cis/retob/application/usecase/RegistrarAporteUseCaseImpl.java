package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final DateTimeFormatter PERIODO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        var existing = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        var monto = command.monto();
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        var periodo = LocalDate.now().format(PERIODO_FORMAT);
        var saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        var nuevoTotal = saldo.calcularNuevoTotal(monto);
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(
                    "El aporte supera el tope mensual de " + topeMensual);
        }

        var marcadaRevision = monto.compareTo(umbralRevision) > 0;

        var saldoActualizado = saldo.conTotal(nuevoTotal);
        saldoRepository.guardar(saldoActualizado);

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

        return aporteRepository.guardar(aporte);
    }
}

package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;

    public RegistrarAporteUseCaseImpl(AporteRepositoryPort aporteRepository, SaldoRepositoryPort saldoRepository) {
        this.aporteRepository = aporteRepository;
        this.saldoRepository = saldoRepository;
    }

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteUseCase.RegistrarAporteCommand command) {
        String afiliadoId = command.afiliadoId();
        BigDecimal monto = command.monto();
        String canal = command.canal();
        String idempotenciaKey = command.idempotenciaKey();

        validarMonto(monto);
        Aporte aporteExistente = aporteRepository.findByIdempotenciaKey(idempotenciaKey).orElse(null);
        if (aporteExistente != null) {
            return aporteExistente;
        }

        LocalDate fecha = LocalDate.now();
        String periodo = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        boolean marcadaRevision = monto.compareTo(umbralRevision) > 0;

        Aporte aporte = new Aporte(
                null, afiliadoId, monto, fecha, canal, periodo, marcadaRevision, idempotenciaKey
        );

        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(afiliadoId, periodo)
                .orElseGet(() -> saldoRepository.inicializar(afiliadoId, periodo));

        BigDecimal nuevoTotal = saldo.getTotal().add(monto);
        validarTopeMensual(nuevoTotal);
        SaldoMensual actualizado = saldo.conTotal(nuevoTotal);
        saldoRepository.guardar(actualizado);

        return aporteRepository.guardar(aporte);
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del aporte debe ser mayor a cero");
        }
    }

    private void validarTopeMensual(BigDecimal nuevoTotal) {
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(
                    "El aporte supera el tope mensual permitido de " + topeMensual);
        }
    }
}
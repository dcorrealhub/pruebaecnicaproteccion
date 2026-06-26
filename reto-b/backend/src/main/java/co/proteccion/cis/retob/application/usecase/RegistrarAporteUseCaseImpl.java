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
import java.time.YearMonth;

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
        // 1. Idempotencia: si ya existe un aporte con esta clave, retornarlo sin efecto
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Monto debe ser estrictamente positivo
        if (command.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        // 3. Cargar o inicializar el saldo mensual del afiliado
        String periodo = YearMonth.now().toString();
        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

        // 4. Validar tope mensual configurable
        BigDecimal nuevoTotal = saldo.getTotal().add(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(
                    "El aporte supera el tope mensual permitido de " + topeMensual +
                    ". Acumulado actual: " + saldo.getTotal());
        }

        // 5. Determinar si requiere revision por superar el umbral
        boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;

        // 6. Construir y persistir el aporte
        Aporte aporte = new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                LocalDate.now(),
                command.canal(),
                periodo,
                marcadaRevision,
                command.idempotenciaKey()
        );
        Aporte aportePersistido = aporteRepository.guardar(aporte);

        // 7. Actualizar el saldo mensual
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return aportePersistido;
    }
}

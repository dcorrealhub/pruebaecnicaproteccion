package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
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

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;
    private final EventoAporteRepositoryPort eventoRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        // 1. Idempotencia: si ya existe, retornar el aporte original
        var existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Validar monto positivo
        if (command.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        // 3. Calcular periodo actual y obtener saldo mensual
        LocalDate hoy = LocalDate.now();
        String periodoActual = hoy.format(PERIODO_FMT);

        SaldoMensual saldo = saldoRepository
                .findByAfiliadoIdAndMes(command.afiliadoId(), periodoActual)
                .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodoActual));

        // 4. Validar tope mensual
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(
                    "El aporte supera el tope mensual permitido de " + topeMensual);
        }

        // 5. Marcar para revisión si supera el umbral
        boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;

        // 6. Actualizar saldo (aquí @Version garantiza control de concurrencia optimista)
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        // 7. Persistir aporte
        Aporte aporte = aporteRepository.guardar(new Aporte(
                null,
                command.afiliadoId(),
                command.monto(),
                hoy,
                command.canal(),
                periodoActual,
                marcadaRevision,
                command.idempotenciaKey()
        ));

        // 8. Registrar evento de auditoría (siempre después del aporte)
        eventoRepository.registrarEvento(aporte.getId(), "APORTE_REGISTRADO");

        return aporte;
    }
}

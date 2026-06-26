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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrarAporteUseCaseImpl implements RegistrarAporteUseCase {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AporteRepositoryPort aporteRepository;
    private final SaldoRepositoryPort saldoRepository;

    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    @Value("${aporte.umbral-revision:5000000}")
    private BigDecimal umbralRevision;

    @Override
    @Transactional
    public Aporte registrar(RegistrarAporteCommand command) {
        // Idempotencia: reintentos con la misma clave devuelven el aporte original sin duplicarlo.
        Optional<Aporte> existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        // Normalizar a mayúsculas para que "af-001" y "AF-001" sean el mismo afiliado en toda la capa.
        String afiliadoId = command.afiliadoId().toUpperCase();

        LocalDate hoy = LocalDate.now();
        // El periodo en formato YYYY-MM sirve como clave del saldo mensual y para consultas por rango.
        String periodo = hoy.format(PERIODO_FMT);

        // Si es el primer aporte del afiliado en el mes, se crea el saldo en cero antes de acumular.
        SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(afiliadoId, periodo)
                .orElseGet(() -> saldoRepository.inicializar(afiliadoId, periodo));

        // El tope se evalúa sobre el acumulado resultante, no solo sobre el monto del aporte.
        BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(String.format(
                    "El aporte supera el tope mensual de %,.0f COP. Acumulado actual: %,.0f COP",
                    topeMensual, saldo.getTotal()));
        }

        // Un aporte que supera el umbral queda marcado para revisión manual posterior.
        boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;

        // El @Version en SaldoMensualEntity lanza OptimisticLockException si dos transacciones
        // concurrentes intentan actualizar el mismo saldo; la segunda debe reintentar.
        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        // id=null indica INSERT; el constructor valida que el monto sea positivo antes de persistir.
        Aporte aporte = new Aporte(
                null,
                afiliadoId,
                command.monto(),
                hoy,
                command.canal(),
                periodo,
                marcadaRevision,
                command.idempotenciaKey()
        );

        return aporteRepository.guardar(aporte);
    }
}

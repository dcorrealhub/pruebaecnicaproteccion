package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
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
    public Aporte registrar(String afiliadoId, BigDecimal monto, String canal, String idempotenciaKey) {
        var aporteExistente = aporteRepository.findByIdempotenciaKey(idempotenciaKey);
        if (aporteExistente.isPresent()) {
            return aporteExistente.get();
        }

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        var hoy = LocalDate.now();
        var periodo = YearMonth.from(hoy).toString();

        var saldo = saldoRepository.findByAfiliadoIdAndMes(afiliadoId, periodo)
                .orElseGet(() -> saldoRepository.inicializar(afiliadoId, periodo));

        var nuevoTotal = saldo.calcularNuevoTotal(monto);
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException(
                    "El aporte excede el tope mensual de " + topeMensual + " para el afiliado " + afiliadoId
            );
        }

        var marcadaRevision = monto.compareTo(umbralRevision) > 0;

        var aporte = new Aporte(
                null,
                afiliadoId,
                monto,
                hoy,
                canal,
                periodo,
                marcadaRevision,
                idempotenciaKey
        );

        var aporteGuardado = aporteRepository.guardar(aporte);

        saldoRepository.guardar(saldo.conTotal(nuevoTotal));

        return aporteGuardado;
    }
}

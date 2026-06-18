package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementación del caso de uso de registro de aportes.
 *
 * TODO (candidato): implementar la lógica de negocio:
 *   1. Verificar idempotencia: si ya existe un aporte con la misma idempotenciaKey, retornarlo.
 *   2. Validar monto positivo y que no supere el tope mensual del afiliado.
 *   3. Marcar para revisión si el monto supera {@code umbralRevision}.
 *   4. Actualizar el saldo mensual del afiliado de forma concurrentemente segura.
 *   5. Persistir el aporte y publicar el evento correspondiente.
 *   6. Envolver todo en una transacción (@Transactional).
 */
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
    public Aporte registrar(RegistrarAporteCommand command) {
        // TODO: implementar
        throw new UnsupportedOperationException("Pendiente de implementación");
    }
}

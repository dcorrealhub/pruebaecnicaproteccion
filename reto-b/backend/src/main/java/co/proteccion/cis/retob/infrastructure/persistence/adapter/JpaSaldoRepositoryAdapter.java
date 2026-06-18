package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link SaldoRepositoryPort}.
 *
 * TODO (candidato): implementar los métodos.
 * Asegúrate de propagar {@link jakarta.persistence.OptimisticLockException}
 * correctamente para manejar conflictos de concurrencia.
 */
@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        // TODO: buscar y mapear
        throw new UnsupportedOperationException("Pendiente de implementación");
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        // TODO: mapear SaldoMensual → SaldoMensualEntity, guardar, mapear de vuelta
        throw new UnsupportedOperationException("Pendiente de implementación");
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        // TODO: crear un saldo con total=0 y persistirlo
        throw new UnsupportedOperationException("Pendiente de implementación");
    }
}

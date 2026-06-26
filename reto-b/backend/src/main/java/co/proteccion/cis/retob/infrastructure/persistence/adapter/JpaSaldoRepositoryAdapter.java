package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link SaldoRepositoryPort}.
 *
 * <p>El control de concurrencia optimista lo aporta la columna {@code @Version}
 * de {@link SaldoMensualEntity}: si dos transacciones intentan actualizar el mismo
 * saldo, JPA lanzará {@link jakarta.persistence.OptimisticLockException} en el flush,
 * que el caso de uso reintenta.
 */
@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes).map(this::aDominio);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity guardada = springDataRepo.save(aEntidad(saldo));
        return aDominio(guardada);
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity nueva = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .build();
        return aDominio(springDataRepo.save(nueva));
    }

    private SaldoMensualEntity aEntidad(SaldoMensual s) {
        return SaldoMensualEntity.builder()
                .id(s.getId())
                .afiliadoId(s.getAfiliadoId())
                .mes(s.getMes())
                .total(s.getTotal())
                .version(s.getVersion())
                .build();
    }

    private SaldoMensual aDominio(SaldoMensualEntity e) {
        return new SaldoMensual(e.getId(), e.getAfiliadoId(), e.getMes(), e.getTotal(), e.getVersion());
    }
}

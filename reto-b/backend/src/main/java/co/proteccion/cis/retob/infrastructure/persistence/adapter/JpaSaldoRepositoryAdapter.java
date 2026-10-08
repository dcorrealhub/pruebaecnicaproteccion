package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link SaldoRepositoryPort}.
 *
 * <p>El control de concurrencia es optimista: la entidad {@link SaldoMensualEntity} lleva
 * {@code @Version}. Al guardar un saldo cuya versión no coincide con la almacenada, Hibernate
 * lanza una {@link org.springframework.orm.ObjectOptimisticLockingFailureException}, que el
 * caso de uso captura para reintentar.</p>
 */
@Repository
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    public JpaSaldoRepositoryAdapter(SpringDataSaldoRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes)
                .map(this::aDominio);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity entity = new SaldoMensualEntity(
                saldo.getId(),
                saldo.getAfiliadoId(),
                saldo.getMes(),
                saldo.getTotal(),
                saldo.getVersion());
        return aDominio(springDataRepo.save(entity));
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity entity = new SaldoMensualEntity(
                null,
                afiliadoId,
                mes,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN),
                null);
        return aDominio(springDataRepo.save(entity));
    }

    private SaldoMensual aDominio(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion());
    }
}

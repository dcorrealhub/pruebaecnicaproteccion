package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link SaldoRepositoryPort}.
 * Traduce los conflictos de versión (y la carrera al crear el saldo del mes)
 * a {@link ConflictoConcurrenciaException}, para que el dominio no dependa de JPA.
 */
@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes).map(JpaSaldoRepositoryAdapter::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        try {
            // La versión leída viaja en la entidad: si otra transacción la cambió,
            // Hibernate emite UPDATE ... WHERE version = ? sin filas afectadas y falla.
            return toDomain(springDataRepo.saveAndFlush(toEntity(saldo)));
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException(e);
        }
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        try {
            SaldoMensualEntity nuevo = SaldoMensualEntity.builder()
                    .afiliadoId(afiliadoId)
                    .mes(mes)
                    .total(BigDecimal.ZERO.setScale(2))
                    .build();
            return toDomain(springDataRepo.saveAndFlush(nuevo));
        } catch (DataIntegrityViolationException e) {
            if (RestriccionesBd.violoRestriccion(e, RestriccionesBd.UQ_SALDO_AFILIADO_MES)) {
                throw new ConflictoConcurrenciaException(e);
            }
            throw e;
        }
    }

    private static SaldoMensualEntity toEntity(SaldoMensual saldo) {
        return SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())
                .build();
    }

    private static SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(entity.getId(), entity.getAfiliadoId(), entity.getMes(),
                entity.getTotal(), entity.getVersion());
    }
}

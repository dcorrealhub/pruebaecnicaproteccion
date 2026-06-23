package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.exception.ConcurrenciaSaldoException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.mapper.SaldoPersistenceMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;
    private final SaldoPersistenceMapper mapper;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes).map(mapper::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        try {
            SaldoMensualEntity entity = mapper.toEntity(saldo);
            return mapper.toDomain(springDataRepo.save(entity));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new ConcurrenciaSaldoException(saldo.getAfiliadoId(), saldo.getMes());
        }
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        try {
            SaldoMensualEntity entity = SaldoMensualEntity.builder()
                    .afiliadoId(afiliadoId)
                    .mes(mes)
                    .total(BigDecimal.ZERO)
                    .build();
            return mapper.toDomain(springDataRepo.save(entity));
        } catch (DataIntegrityViolationException ex) {
            return findByAfiliadoIdAndMes(afiliadoId, mes)
                    .orElseThrow(() -> ex);
        }
    }
}

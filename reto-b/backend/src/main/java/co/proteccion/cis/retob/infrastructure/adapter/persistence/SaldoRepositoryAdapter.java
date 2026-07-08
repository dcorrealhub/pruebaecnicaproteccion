package co.proteccion.cis.retob.infrastructure.adapter.persistence;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.mapper.SaldoMensualMapper;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.repository.SaldoMensualJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SaldoMensualJpaRepository jpaRepository;
    private final SaldoMensualMapper mapper;

    public SaldoRepositoryAdapter(SaldoMensualJpaRepository jpaRepository, SaldoMensualMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return jpaRepository.findByAfiliadoIdAndMes(afiliadoId, mes).map(mapper::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity entity = mapper.toEntity(saldo);
        try {
            SaldoMensualEntity guardado = jpaRepository.save(entity);
            return mapper.toDomain(guardado);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Conflicto de concurrencia al actualizar el saldo mensual", ex);
        }
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity entity = new SaldoMensualEntity(null, afiliadoId, mes, BigDecimal.ZERO, 0);
        SaldoMensualEntity guardado = jpaRepository.save(entity);
        return mapper.toDomain(guardado);
    }
}
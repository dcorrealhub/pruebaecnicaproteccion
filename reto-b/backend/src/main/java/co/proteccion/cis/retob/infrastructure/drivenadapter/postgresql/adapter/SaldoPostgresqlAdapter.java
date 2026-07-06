package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.adapter;

import co.proteccion.cis.retob.domain.model.aporte.SaldoMensual;
import co.proteccion.cis.retob.domain.model.aporte.gateway.SaldoOutputPort;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper.SaldoEntityMapper;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.SaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SaldoPostgresqlAdapter implements SaldoOutputPort {

    private final SaldoRepository repository;
    private final SaldoEntityMapper mapper;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return repository.findByAfiliadoIdAndMes(afiliadoId, mes).map(mapper::toDomain);
    }

    @Override
    public SaldoMensual save(SaldoMensual saldo) {
        return mapper.toDomain(repository.save(mapper.toEntity(saldo)));
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity nueva = new SaldoMensualEntity();
        nueva.setAfiliadoId(afiliadoId);
        nueva.setMes(mes);
        nueva.setTotal(BigDecimal.ZERO);
        return mapper.toDomain(repository.save(nueva));
    }
}

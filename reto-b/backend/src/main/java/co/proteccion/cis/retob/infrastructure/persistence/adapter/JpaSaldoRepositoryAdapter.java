package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private final SpringDataSaldoRepository springDataRepo;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes)
                .map(JpaSaldoRepositoryAdapter::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity entity = toEntity(saldo);
        SaldoMensualEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity entity = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .version(0)
                .build();
        SaldoMensualEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    private static SaldoMensualEntity toEntity(SaldoMensual domain) {
        return SaldoMensualEntity.builder()
                .id(domain.getId())
                .afiliadoId(domain.getAfiliadoId())
                .mes(domain.getMes())
                .total(domain.getTotal())
                .version(domain.getVersion())
                .build();
    }

    private static SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion()
        );
    }
}

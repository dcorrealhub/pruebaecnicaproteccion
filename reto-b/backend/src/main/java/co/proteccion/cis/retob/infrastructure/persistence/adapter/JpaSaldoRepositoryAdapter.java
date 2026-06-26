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
                .map(this::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        SaldoMensualEntity entity = toEntity(saldo);
        SaldoMensualEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity nueva = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .version(0)
                .build();
        return toDomain(springDataRepo.save(nueva));
    }

    private SaldoMensualEntity toEntity(SaldoMensual s) {
        return SaldoMensualEntity.builder()
                .id(s.getId())
                .afiliadoId(s.getAfiliadoId())
                .mes(s.getMes())
                .total(s.getTotal())
                .version(s.getVersion())
                .build();
    }

    private SaldoMensual toDomain(SaldoMensualEntity e) {
        return new SaldoMensual(
                e.getId(),
                e.getAfiliadoId(),
                e.getMes(),
                e.getTotal(),
                e.getVersion()
        );
    }
}
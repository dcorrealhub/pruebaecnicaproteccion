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
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes).map(this::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        // El campo version se propaga para que Hibernate verifique el bloqueo optimista en el UPDATE.
        SaldoMensualEntity entity = SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())
                .build();
        return toDomain(springDataRepo.save(entity));
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        // Crea la fila de saldo para el mes con total en cero; version=0 es el punto de partida.
        SaldoMensualEntity entity = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .version(0)
                .build();
        return toDomain(springDataRepo.save(entity));
    }

    // Convierte la entidad JPA al modelo de dominio para aislar la persistencia del dominio.
    private SaldoMensual toDomain(SaldoMensualEntity e) {
        return new SaldoMensual(e.getId(), e.getAfiliadoId(), e.getMes(), e.getTotal(), e.getVersion());
    }
}

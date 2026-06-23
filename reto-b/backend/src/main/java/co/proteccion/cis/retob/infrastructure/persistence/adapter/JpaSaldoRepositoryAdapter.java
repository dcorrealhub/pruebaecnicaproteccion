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
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes)
                .map(this::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        // TODO: mapear SaldoMensual → SaldoMensualEntity, guardar, mapear de vuelta
        // OptimisticLockException se propaga automáticamente via @Version en la entity
        SaldoMensualEntity entity = toEntity(saldo);
        SaldoMensualEntity guardado = springDataRepo.save(entity);
        return toDomain(guardado);
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        // TODO: crear un saldo con total=0 y persistirlo
        SaldoMensualEntity entity = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .build();
        SaldoMensualEntity guardado = springDataRepo.save(entity);
        return toDomain(guardado);
    }

    // Mapeos

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
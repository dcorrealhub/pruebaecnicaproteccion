package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
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
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes)
                .map(this::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        // @Version en SaldoMensualEntity hace que JPA lance OptimisticLockException
        // automaticamente si el 'version' no coincide con el de la base de datos.
        // No se captura aqui a proposito: debe propagarse para que el caso de uso decida si reintenta.
        SaldoMensualEntity entity = toEntity(saldo);
        SaldoMensualEntity guardado = springDataRepo.saveAndFlush(entity);
        return toDomain(guardado);
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        SaldoMensualEntity nuevo = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .build();
        SaldoMensualEntity guardado = springDataRepo.save(nuevo);
        return toDomain(guardado);
    }

    private SaldoMensualEntity toEntity(SaldoMensual saldo) {
        return SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())
                .build();
    }

    private SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion()
        );
    }
}
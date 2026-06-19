package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        return toDomain(springDataRepo.save(toEntity(saldo)));
    }

    /**
     * Crea un saldo en cero para afiliado/mes. Si dos threads concurrentes llegan aquí
     * simultáneamente, la constraint UNIQUE (afiliado_id, mes) provoca un
     * DataIntegrityViolationException en el segundo: lo absorbemos y leemos el registro
     * que ya insertó el primer thread (patrón find-or-create tolerante a race condition).
     */
    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        try {
            SaldoMensualEntity nueva = SaldoMensualEntity.builder()
                    .afiliadoId(afiliadoId)
                    .mes(mes)
                    .total(BigDecimal.ZERO)
                    .build();
            return toDomain(springDataRepo.save(nueva));
        } catch (DataIntegrityViolationException e) {
            return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes)
                    .map(this::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se pudo inicializar el saldo mensual para " + afiliadoId + "/" + mes, e));
        }
    }

    private SaldoMensual toDomain(SaldoMensualEntity e) {
        return new SaldoMensual(e.getId(), e.getAfiliadoId(), e.getMes(), e.getTotal(), e.getVersion());
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
}

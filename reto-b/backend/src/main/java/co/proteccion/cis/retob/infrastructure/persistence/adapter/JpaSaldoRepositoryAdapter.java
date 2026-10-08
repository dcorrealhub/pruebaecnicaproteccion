package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.mappers.SaldoMensualMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link SaldoRepositoryPort}.
 *
 * <p>Control de concurrencia optimista: el {@code @Version} de la entidad hace que una
 * actualización sobre una versión desactualizada falle. Ese fallo, y la carrera al crear
 * el saldo del mes (restricción única afiliado + mes), se traducen a
 * {@link ConcurrenciaConflictoException}. Se usa {@code saveAndFlush} para que el conflicto
 * se detecte aquí y no al hacer commit, fuera del adaptador.
 */
@Repository
@RequiredArgsConstructor
public class JpaSaldoRepositoryAdapter implements SaldoRepositoryPort {

    private static final String UQ_SALDO_AFILIADO_MES = "uq_saldo_afiliado_mes";

    private final SpringDataSaldoRepository springDataRepo;

    @Override
    public Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes) {
        return springDataRepo.findByAfiliadoIdAndMes(afiliadoId, mes).map(SaldoMensualMapper::toDomain);
    }

    @Override
    public SaldoMensual guardar(SaldoMensual saldo) {
        try {
            return SaldoMensualMapper.toDomain(springDataRepo.saveAndFlush(SaldoMensualMapper.toEntity(saldo)));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrenciaConflictoException(
                    "El saldo mensual fue modificado por otra operación concurrente; reintente la operación", e);
        }
    }

    @Override
    public SaldoMensual inicializar(String afiliadoId, String mes) {
        var nuevo = SaldoMensualEntity.builder()
                .afiliadoId(afiliadoId)
                .mes(mes)
                .total(BigDecimal.ZERO)
                .build();
        try {
            return SaldoMensualMapper.toDomain(springDataRepo.saveAndFlush(nuevo));
        } catch (DataIntegrityViolationException e) {
            if (ViolacionRestriccion.es(e, UQ_SALDO_AFILIADO_MES)) {
                throw new ConcurrenciaConflictoException(
                        "El saldo mensual fue creado por otra operación concurrente; reintente la operación", e);
            }
            throw e;
        }
    }
}

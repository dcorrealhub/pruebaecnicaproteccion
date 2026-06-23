package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.port.out.IdempotenciaRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.IdempotenciaAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoIdempotencia;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataIdempotenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaIdempotenciaRepositoryAdapter implements IdempotenciaRepositoryPort {

    private final SpringDataIdempotenciaRepository springDataRepo;

    @Override
    public Optional<Registro> findByKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(this::toRegistro);
    }

    @Override
    public boolean intentarClaim(String idempotenciaKey) {
        try {
            var entity = IdempotenciaAporteEntity.builder()
                    .idempotenciaKey(idempotenciaKey)
                    .estado(EstadoIdempotencia.EN_PROCESO)
                    .build();
            springDataRepo.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public void completar(String idempotenciaKey, Long aporteId) {
        springDataRepo.findByIdempotenciaKey(idempotenciaKey).ifPresent(entity -> {
            entity.setAporteId(aporteId);
            entity.setEstado(EstadoIdempotencia.COMPLETADO);
            springDataRepo.save(entity);
        });
    }

    @Override
    @Transactional
    public void liberarClaim(String idempotenciaKey) {
        springDataRepo.findByIdempotenciaKey(idempotenciaKey).ifPresent(entity -> {
            if (entity.getEstado() == EstadoIdempotencia.EN_PROCESO) {
                springDataRepo.delete(entity);
            }
        });
    }

    private Registro toRegistro(IdempotenciaAporteEntity entity) {
        return new Registro(
                entity.getIdempotenciaKey(),
                entity.getAporteId(),
                mapEstado(entity.getEstado())
        );
    }

    private Estado mapEstado(EstadoIdempotencia estado) {
        return estado == EstadoIdempotencia.COMPLETADO ? Estado.COMPLETADO : Estado.EN_PROCESO;
    }
}

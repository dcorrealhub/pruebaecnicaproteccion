package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.EventoAporte;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.mappers.EventoAporteMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Adaptador JPA para el puerto de salida {@link EventoAporteRepositoryPort}.
 */
@Repository
@RequiredArgsConstructor
public class JpaEventoAporteRepositoryAdapter implements EventoAporteRepositoryPort {

    private final SpringDataEventoAporteRepository springDataRepo;

    @Override
    public EventoAporte guardar(EventoAporte evento) {
        return EventoAporteMapper.toDomain(springDataRepo.save(EventoAporteMapper.toEntity(evento)));
    }
}

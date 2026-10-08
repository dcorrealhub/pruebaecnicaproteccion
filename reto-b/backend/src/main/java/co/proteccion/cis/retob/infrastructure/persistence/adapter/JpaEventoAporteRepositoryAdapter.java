package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
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
    public void registrar(Long aporteId, TipoEventoAporte tipo) {
        springDataRepo.save(EventoAporteEntity.builder()
                .aporteId(aporteId)
                .tipo(tipo.name())
                .build());
    }
}

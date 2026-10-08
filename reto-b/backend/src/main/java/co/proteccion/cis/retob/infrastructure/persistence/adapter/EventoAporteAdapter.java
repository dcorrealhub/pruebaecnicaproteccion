package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoAporteRepository;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia para la trazabilidad de eventos de aporte.
 */
@Repository
public class EventoAporteAdapter implements EventoAportePort {

    private static final String TIPO_REGISTRADO = "APORTE_REGISTRADO";

    private final SpringDataEventoAporteRepository springDataRepo;

    public EventoAporteAdapter(SpringDataEventoAporteRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public void registrarCreado(Aporte aporte) {
        springDataRepo.save(new EventoAporteEntity(null, aporte.getId(), TIPO_REGISTRADO, null));
    }
}

package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.adapter;

import co.proteccion.cis.retob.domain.model.aporte.gateway.EventoOutputPort;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.EventoAporteEntity;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventoPostgresqlAdapter implements EventoOutputPort {

    private final EventoRepository repository;

    @Override
    public void registrar(Long aporteId, Tipo tipo) {
        EventoAporteEntity evento = new EventoAporteEntity();
        evento.setAporteId(aporteId);
        evento.setTipo(tipo.name());
        repository.save(evento);
    }
}

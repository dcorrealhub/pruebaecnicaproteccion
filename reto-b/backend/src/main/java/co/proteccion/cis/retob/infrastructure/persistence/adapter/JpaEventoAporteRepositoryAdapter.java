package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaEventoAporteRepositoryAdapter implements EventoAporteRepositoryPort {

    private final SpringDataEventoAporteRepository eventoRepository;
    private final SpringDataAporteRepository aporteRepository;

    @Override
    public void registrar(Long aporteId, TipoEventoAporte tipo) {
        AporteEntity aporte = aporteRepository.getReferenceById(aporteId);
        EventoAporteEntity evento = EventoAporteEntity.builder()
                .aporte(aporte)
                .tipo(co.proteccion.cis.retob.infrastructure.persistence.enums.TipoEventoAporte
                        .valueOf(tipo.name()))
                .build();
        eventoRepository.save(evento);
    }
}

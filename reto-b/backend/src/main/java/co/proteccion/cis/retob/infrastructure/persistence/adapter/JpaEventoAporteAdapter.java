package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaEventoAporteAdapter implements EventoAportePort {

    private final SpringDataEventoRepository repo;

    @Override
    public void registrar(Long aporteId, Tipo tipo) {
        repo.save(EventoAporteEntity.builder()
                .aporteId(aporteId)
                .tipo(tipo.name())
                .build());
    }
}

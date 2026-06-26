package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaEventoAporteRepositoryAdapter implements EventoAporteRepositoryPort {

    private final SpringDataEventoAporteRepository springDataRepo;

    @Override
    public void registrarEvento(Long aporteId, String tipo) {
        springDataRepo.save(EventoAporteEntity.builder()
                .aporteId(aporteId)
                .tipo(tipo)
                .build());
    }
}

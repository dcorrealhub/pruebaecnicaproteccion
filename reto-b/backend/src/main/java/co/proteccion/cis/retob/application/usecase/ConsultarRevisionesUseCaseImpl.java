package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.RevisionAporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarRevisionesUseCase;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarRevisionesUseCaseImpl implements ConsultarRevisionesUseCase {

    private final RevisionRepository revisionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RevisionAporte> consultar(Long aporteId) {
        return revisionRepository.findByAporteId(aporteId);
    }
}

package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.in.ConsultarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConsultarParametrosUseCaseImpl implements ConsultarParametrosUseCase {

    private final ParametroRepository parametroRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ParametrosFondo> consultarActual() {
        return parametroRepository.findLatest();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametrosFondo> consultarHistorial() {
        return parametroRepository.findAll();
    }
}

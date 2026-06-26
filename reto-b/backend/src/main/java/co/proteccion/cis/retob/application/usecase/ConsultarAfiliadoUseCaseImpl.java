package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.port.in.ConsultarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarAfiliadoUseCaseImpl implements ConsultarAfiliadoUseCase {

    private final AfiliadoRepository afiliadoRepository;

    @Override
    @Transactional(readOnly = true)
    public Afiliado consultar(String afiliadoId) {
        return afiliadoRepository.findByAfiliadoId(afiliadoId)
                .orElseThrow(() -> new AfiliadoNotFoundException(afiliadoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Afiliado> consultarTodos() {
        return afiliadoRepository.findAll();
    }
}

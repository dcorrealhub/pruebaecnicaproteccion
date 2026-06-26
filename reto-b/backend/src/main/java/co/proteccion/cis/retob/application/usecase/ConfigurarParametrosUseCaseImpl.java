package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.port.in.ConfigurarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.out.ParametroAportePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConfigurarParametrosUseCaseImpl implements ConfigurarParametrosUseCase {

    private final ParametroAportePort parametroPort;

    @Override
    @Transactional(readOnly = true)
    public ParametrosAporte obtenerGlobal() {
        return parametroPort.obtenerGlobal();
    }

    @Override
    @Transactional
    public ParametrosAporte actualizarGlobal(BigDecimal topeMensual, BigDecimal umbralRevision) {
        // El constructor de ParametrosAporte valida las invariantes (positivos, umbral ≤ tope).
        return parametroPort.actualizarGlobal(new ParametrosAporte(topeMensual, umbralRevision));
    }
}

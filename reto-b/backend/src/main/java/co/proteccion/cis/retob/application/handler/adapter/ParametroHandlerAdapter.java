package co.proteccion.cis.retob.application.handler.adapter;

import co.proteccion.cis.retob.application.handler.ParametroHandler;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.usecase.input.ParametroInputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParametroHandlerAdapter implements ParametroHandler {

    private final ParametroInputPort parametroInputPort;

    @Override
    public ParametrosAporte obtenerGlobal() {
        return parametroInputPort.obtenerGlobal();
    }

    @Override
    @Transactional
    public ParametrosAporte actualizarGlobal(ParametrosAporte parametros) {
        return parametroInputPort.actualizarGlobal(parametros);
    }
}

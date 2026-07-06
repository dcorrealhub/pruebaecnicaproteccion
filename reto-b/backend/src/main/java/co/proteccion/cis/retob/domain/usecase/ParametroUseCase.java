package co.proteccion.cis.retob.domain.usecase;

import co.proteccion.cis.retob.domain.constant.ParametroMessages;
import co.proteccion.cis.retob.domain.exception.DomainValidationException;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import co.proteccion.cis.retob.domain.usecase.input.ParametroInputPort;
import lombok.RequiredArgsConstructor;

/**
 * Consulta y actualización de los parámetros globales (tope y umbral). Dominio puro.
 */
@RequiredArgsConstructor
public class ParametroUseCase implements ParametroInputPort {

    private final ParametroOutputPort parametroOutputPort;

    @Override
    public ParametrosAporte obtenerGlobal() {
        return parametroOutputPort.obtenerGlobal();
    }

    @Override
    public ParametrosAporte actualizarGlobal(ParametrosAporte parametros) {
        validar(parametros);
        return parametroOutputPort.actualizarGlobal(parametros);
    }

    private void validar(ParametrosAporte p) {
        if (p.topeMensual() == null || p.topeMensual().signum() <= 0) {
            throw new DomainValidationException(ParametroMessages.TOPE_POSITIVO);
        }
        if (p.umbralRevision() == null || p.umbralRevision().signum() <= 0) {
            throw new DomainValidationException(ParametroMessages.UMBRAL_POSITIVO);
        }
        if (p.umbralRevision().compareTo(p.topeMensual()) > 0) {
            throw new DomainValidationException(ParametroMessages.UMBRAL_MAYOR_TOPE);
        }
    }
}

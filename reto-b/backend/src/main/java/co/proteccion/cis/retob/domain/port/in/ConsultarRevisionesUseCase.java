package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.RevisionAporte;

import java.util.List;

public interface ConsultarRevisionesUseCase {

    List<RevisionAporte> consultar(Long aporteId);
}

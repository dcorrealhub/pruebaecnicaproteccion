package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.RevisionAporte;

import java.util.List;

public interface RevisionRepository {

    RevisionAporte guardar(RevisionAporte revision);

    List<RevisionAporte> findByAporteId(Long aporteId);
}

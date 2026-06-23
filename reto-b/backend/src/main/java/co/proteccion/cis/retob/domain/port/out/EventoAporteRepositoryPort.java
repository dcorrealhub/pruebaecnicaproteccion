package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.TipoEventoAporte;

public interface EventoAporteRepositoryPort {

    void registrar(Long aporteId, TipoEventoAporte tipo);
}

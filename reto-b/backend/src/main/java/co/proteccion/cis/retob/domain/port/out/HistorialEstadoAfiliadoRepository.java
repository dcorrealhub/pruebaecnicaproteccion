package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.HistorialEstadoAfiliado;

import java.util.List;

public interface HistorialEstadoAfiliadoRepository {

    List<HistorialEstadoAfiliado> findByAfiliadoId(String afiliadoId);
}

package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Afiliado;

import java.util.List;
import java.util.Optional;

public interface AfiliadoRepository {

    Afiliado guardar(Afiliado afiliado);

    Optional<Afiliado> findByAfiliadoId(String afiliadoId);

    List<Afiliado> findAll();
}

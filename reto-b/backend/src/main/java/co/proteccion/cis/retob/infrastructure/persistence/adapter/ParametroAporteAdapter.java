package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.port.out.ParametroAportePort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Resuelve los parámetros de un afiliado: usa el override por afiliado si existe,
 * de lo contrario el valor global por defecto (fila con afiliado_id NULL).
 */
@Repository
@RequiredArgsConstructor
public class ParametroAporteAdapter implements ParametroAportePort {

    private final SpringDataParametroRepository repo;

    @Override
    public ParametrosAporte forAfiliado(String afiliadoId) {
        ParametroAporteEntity e = repo.findByAfiliadoId(afiliadoId)
                .or(repo::findByAfiliadoIdIsNull)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay parámetros de aporte configurados (falta la fila global por defecto)"));
        return new ParametrosAporte(e.getTopeMensual(), e.getUmbralRevision());
    }

    @Override
    public ParametrosAporte obtenerGlobal() {
        ParametroAporteEntity e = repo.findByAfiliadoIdIsNull()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay parámetros globales configurados (falta la fila por defecto)"));
        return new ParametrosAporte(e.getTopeMensual(), e.getUmbralRevision());
    }

    @Override
    public ParametrosAporte actualizarGlobal(ParametrosAporte params) {
        ParametroAporteEntity e = repo.findByAfiliadoIdIsNull()
                .orElseGet(() -> ParametroAporteEntity.builder().afiliadoId(null).build());
        e.setTopeMensual(params.topeMensual());
        e.setUmbralRevision(params.umbralRevision());
        ParametroAporteEntity guardada = repo.save(e);
        return new ParametrosAporte(guardada.getTopeMensual(), guardada.getUmbralRevision());
    }
}

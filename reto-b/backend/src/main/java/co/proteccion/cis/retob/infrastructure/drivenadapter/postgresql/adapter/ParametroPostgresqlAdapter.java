package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.adapter;

import co.proteccion.cis.retob.domain.constant.ParametroMessages;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper.ParametroEntityMapper;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParametroPostgresqlAdapter implements ParametroOutputPort {

    private final ParametroRepository repository;
    private final ParametroEntityMapper mapper;

    @Override
    public ParametrosAporte forAfiliado(String afiliadoId) {
        ParametroAporteEntity entity = repository.findByAfiliadoId(afiliadoId)
                .or(repository::findByAfiliadoIdIsNull)
                .orElseThrow(() -> new IllegalStateException(ParametroMessages.GLOBAL_NO_CONFIGURADO));
        return mapper.toDomain(entity);
    }

    @Override
    public ParametrosAporte obtenerGlobal() {
        ParametroAporteEntity entity = repository.findByAfiliadoIdIsNull()
                .orElseThrow(() -> new IllegalStateException(ParametroMessages.GLOBAL_NO_CONFIGURADO));
        return mapper.toDomain(entity);
    }

    @Override
    public ParametrosAporte actualizarGlobal(ParametrosAporte parametros) {
        ParametroAporteEntity entity = repository.findByAfiliadoIdIsNull()
                .orElseGet(ParametroAporteEntity::new);
        entity.setTopeMensual(parametros.topeMensual());
        entity.setUmbralRevision(parametros.umbralRevision());
        return mapper.toDomain(repository.save(entity));
    }
}

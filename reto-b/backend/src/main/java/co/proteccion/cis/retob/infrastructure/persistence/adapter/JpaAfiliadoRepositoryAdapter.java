package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AfiliadoEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaAfiliadoRepositoryAdapter implements AfiliadoRepository {

    private final SpringDataAfiliadoRepository springDataRepo;

    @Override
    public Afiliado guardar(Afiliado afiliado) {
        return toDomain(springDataRepo.save(toEntity(afiliado)));
    }

    @Override
    public Optional<Afiliado> findByAfiliadoId(String afiliadoId) {
        return springDataRepo.findByAfiliadoId(afiliadoId).map(this::toDomain);
    }

    @Override
    public List<Afiliado> findAll() {
        return springDataRepo.findAll().stream().map(this::toDomain).toList();
    }

    private AfiliadoEntity toEntity(Afiliado a) {
        return AfiliadoEntity.builder()
                .id(a.getId() != null ? UUID.fromString(a.getId()) : null)
                .afiliadoId(a.getAfiliadoId())
                .nombre(a.getNombre())
                .estado(a.getEstado())
                .creadoEn(a.getCreadoEn())
                .build();
    }

    private Afiliado toDomain(AfiliadoEntity e) {
        return new Afiliado(e.getId().toString(), e.getAfiliadoId(), e.getNombre(), e.getEstado(), e.getCreadoEn());
    }
}

package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para {@link AporteRepositoryPort}.
 *
 * Responsabilidad única: traducir entre el modelo de dominio {@link Aporte}
 * y la entidad JPA {@link AporteEntity}, delegando la persistencia en
 * {@link SpringDataAporteRepository}. No contiene lógica de negocio.
 */
@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    // -------------------------------------------------------------------------
    // Puerto de salida — implementación
    // -------------------------------------------------------------------------

    /**
     * Persiste un aporte nuevo o actualiza uno existente.
     *
     * El campo {@code id} llega como {@code null} en aportes nuevos;
     * JPA asigna el valor generado por la secuencia IDENTITY de la BD.
     * El campo {@code creadoEn} de la entidad lo gestiona {@code @PrePersist}.
     */
    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity entidad = toEntity(aporte);
        AporteEntity guardada = springDataRepo.save(entidad);
        return toDomain(guardada);
    }

    /**
     * Busca un aporte por su clave de idempotencia.
     * Retorna {@link Optional#empty()} si no existe — la decisión de qué hacer
     * con ese caso pertenece al caso de uso, no al adaptador.
     */
    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo
                .findByIdempotenciaKey(idempotenciaKey)
                .map(this::toDomain);
    }

    /**
     * Recupera todos los aportes de un afiliado cuyo periodo (YYYY-MM) esté
     * dentro del rango [periodoDesde, periodoHasta], ambos inclusive.
     *
     * La comparación lexicográfica de {@code String} en formato YYYY-MM
     * coincide con el orden cronológico, por lo que {@code BETWEEN} de
     * Spring Data produce el resultado correcto.
     */
    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Métodos privados de mapeo — sin lógica de negocio
    // -------------------------------------------------------------------------

    /**
     * Convierte un objeto de dominio {@link Aporte} en una entidad JPA.
     *
     * {@code creadoEn} se deja en {@code null}: el callback {@code @PrePersist}
     * de {@link AporteEntity} lo asigna automáticamente antes de persistir.
     * La capa de dominio no conoce ni debe conocer ese campo de auditoría.
     */
    private AporteEntity toEntity(Aporte aporte) {
        return AporteEntity.builder()
                .id(aporte.getId())
                .afiliadoId(aporte.getAfiliadoId())
                .monto(aporte.getMonto())
                .fecha(aporte.getFecha())
                .canal(aporte.getCanal())
                .periodo(aporte.getPeriodo())
                .marcadaRevision(aporte.isMarcadaRevision())
                .idempotenciaKey(aporte.getIdempotenciaKey())
                .creadoEn(null)   // gestionado por @PrePersist en AporteEntity
                .build();
    }

    /**
     * Convierte una entidad JPA {@link AporteEntity} en el objeto de dominio.
     *
     * {@code creadoEn} se descarta intencionalmente: es metadata de
     * infraestructura que el dominio no modela ni necesita.
     */
    private Aporte toDomain(AporteEntity entidad) {
        return new Aporte(
                entidad.getId(),
                entidad.getAfiliadoId(),
                entidad.getMonto(),
                entidad.getFecha(),
                entidad.getCanal(),
                entidad.getPeriodo(),
                entidad.isMarcadaRevision(),
                entidad.getIdempotenciaKey()
        );
    }
}

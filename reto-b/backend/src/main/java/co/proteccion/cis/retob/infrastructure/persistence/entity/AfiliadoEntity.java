package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "afiliado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfiliadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "afiliado_id", nullable = false, unique = true, length = 50)
    private String afiliadoId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAfiliado estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) creadoEn = OffsetDateTime.now();
    }
}

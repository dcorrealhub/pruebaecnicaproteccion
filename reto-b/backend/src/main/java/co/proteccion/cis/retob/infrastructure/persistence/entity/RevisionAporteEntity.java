package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.domain.model.EstadoAporte;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "revision_aporte")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aporte_id", nullable = false, columnDefinition = "UUID")
    private AporteEntity aporte;

    @Column(nullable = false, length = 100)
    private String revisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAporte decision;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    @PrePersist
    void prePersist() {
        if (ocurridoEn == null) ocurridoEn = OffsetDateTime.now();
    }
}

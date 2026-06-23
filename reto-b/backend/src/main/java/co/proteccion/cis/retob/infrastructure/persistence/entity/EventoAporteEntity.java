package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.infrastructure.persistence.enums.TipoEventoAporte;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "evento_aporte",
        indexes = @Index(name = "idx_evento_aporte_id", columnList = "aporte_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aporte_id", nullable = false, updatable = false)
    private AporteEntity aporte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TipoEventoAporte tipo;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private OffsetDateTime ocurridoEn;

    @PrePersist
    void prePersist() {
        if (ocurridoEn == null) {
            ocurridoEn = OffsetDateTime.now();
        }
    }
}

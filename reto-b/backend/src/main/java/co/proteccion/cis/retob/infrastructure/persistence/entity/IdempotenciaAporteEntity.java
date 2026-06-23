package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoIdempotencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "idempotencia_aporte",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idempotencia_aporte_key",
                columnNames = "idempotencia_key"
        ),
        indexes = @Index(
                name = "idx_idempotencia_aporte_key",
                columnList = "idempotencia_key",
                unique = true
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotenciaAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotencia_key", nullable = false, unique = true, length = 100, updatable = false)
    private String idempotenciaKey;

    @Column(name = "aporte_id")
    private Long aporteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIdempotencia estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoIdempotencia.EN_PROCESO;
        }
    }
}

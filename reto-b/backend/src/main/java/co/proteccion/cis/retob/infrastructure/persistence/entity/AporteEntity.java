package co.proteccion.cis.retob.infrastructure.persistence.entity;

import co.proteccion.cis.retob.infrastructure.persistence.enums.CanalAporte;
import co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoAporte;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "aporte",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aporte_idempotencia",
                columnNames = "idempotencia_key"
        ),
        indexes = @Index(
                name = "idx_aporte_afiliado_periodo",
                columnList = "afiliado_id, periodo"
        )
)
@Check(constraints = "monto > 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50, updatable = false)
    private String afiliadoId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private CanalAporte canal;

    @Column(nullable = false, length = 7, updatable = false)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoAporte estado;

    @Column(name = "idempotencia_key", nullable = false, unique = true, length = 100, updatable = false)
    private String idempotenciaKey;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoAporte.REGISTRADO;
        }
    }
}

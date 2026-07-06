package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity;

import co.proteccion.cis.retob.domain.model.aporte.EstadoAporte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AporteEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "canal", nullable = false, length = 50)
    private String canal;

    @Column(name = "periodo", nullable = false, length = 7)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAporte estado;

    @Column(name = "idempotencia_key", nullable = false, unique = true, length = 100)
    private String idempotenciaKey;
}

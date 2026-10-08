package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "parametro_afiliado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroAfiliadoEntity {

    @Id
    @Column(name = "afiliado_id", length = 50)
    private String afiliadoId;

    /** {@code null}: aplica el tope por defecto. */
    @Column(name = "tope_mensual", precision = 15, scale = 2)
    private BigDecimal topeMensual;

    /** {@code null}: aplica el umbral por defecto. */
    @Column(name = "umbral_revision", precision = 15, scale = 2)
    private BigDecimal umbralRevision;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void marcarActualizacion() {
        actualizadoEn = OffsetDateTime.now();
    }
}

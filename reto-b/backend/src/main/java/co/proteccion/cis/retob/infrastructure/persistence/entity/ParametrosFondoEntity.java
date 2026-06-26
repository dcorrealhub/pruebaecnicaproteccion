package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historico_parametros")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametrosFondoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tope_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal topeMensual;

    @Column(name = "umbral_revision", nullable = false, precision = 15, scale = 2)
    private BigDecimal umbralRevision;

    @Column(name = "modificado_por", nullable = false, length = 100)
    private String modificadoPor;

    @Column(name = "modificado_en", nullable = false)
    private OffsetDateTime modificadoEn;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @PrePersist
    void prePersist() {
        if (modificadoEn == null) {
            modificadoEn = OffsetDateTime.now();
        }
    }
}

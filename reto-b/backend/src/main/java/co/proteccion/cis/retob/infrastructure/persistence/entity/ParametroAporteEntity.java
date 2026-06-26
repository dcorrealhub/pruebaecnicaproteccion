package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Parámetros configurables de aportes. La fila con {@code afiliadoId == null} es
 * el valor global por defecto; filas con afiliado lo sobreescriben.
 */
@Entity
@Table(name = "parametro_aporte")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", length = 50)
    private String afiliadoId;

    @Column(name = "tope_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal topeMensual;

    @Column(name = "umbral_revision", nullable = false, precision = 15, scale = 2)
    private BigDecimal umbralRevision;
}

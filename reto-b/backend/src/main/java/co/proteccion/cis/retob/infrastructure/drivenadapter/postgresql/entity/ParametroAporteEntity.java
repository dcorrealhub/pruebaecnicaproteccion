package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "parametro_aporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParametroAporteEntity extends BaseEntity {

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

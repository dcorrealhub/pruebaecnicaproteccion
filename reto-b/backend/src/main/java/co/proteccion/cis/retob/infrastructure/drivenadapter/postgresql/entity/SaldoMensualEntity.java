package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "saldo_mensual",
        uniqueConstraints = @UniqueConstraint(columnNames = {"afiliado_id", "mes"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaldoMensualEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(name = "mes", nullable = false, length = 7)
    private String mes;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}

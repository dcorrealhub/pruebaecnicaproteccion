package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
        name = "saldo_mensual",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_saldo_afiliado_mes",
                columnNames = {"afiliado_id", "mes"}
        )
)
@Check(constraints = "total >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaldoMensualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, length = 7)
    private String mes;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Integer version;
}

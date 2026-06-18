package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "saldo_mensual",
       uniqueConstraints = @UniqueConstraint(columnNames = {"afiliado_id", "mes"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoMensualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, length = 7)
    private String mes;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Version
    @Column(nullable = false)
    private Integer version;
}

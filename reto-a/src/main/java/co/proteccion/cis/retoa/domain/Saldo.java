package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "saldo", uniqueConstraints = @UniqueConstraint(columnNames = {"afiliado_id", "mes"}))
@Getter
@Setter
@NoArgsConstructor
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    // Acumulado del mes en pesos colombianos
    private BigDecimal totalMes;

    // Formato YYYY-MM
    private String mes;
}

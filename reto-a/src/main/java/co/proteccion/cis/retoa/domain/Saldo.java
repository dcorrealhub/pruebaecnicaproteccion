package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "saldo")
@Data
@NoArgsConstructor
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    // Acumulado del mes en pesos colombianos
    @Column(precision = 19, scale = 2)
    private BigDecimal totalMes;

    // Formato YYYY-MM
    private String mes;
}

package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private double totalMes;

    // Formato YYYY-MM
    private String mes;
}

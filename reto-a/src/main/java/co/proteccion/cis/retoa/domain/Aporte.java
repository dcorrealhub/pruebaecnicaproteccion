package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    // Representa el monto del aporte en pesos colombianos
    @Column(precision = 19, scale = 2)
    private BigDecimal monto;

    private LocalDate fecha;

    private String canal;

    // Formato YYYY-MM, derivado de la fecha de registro
    private String periodo;

    private boolean marcadaRevision;
}
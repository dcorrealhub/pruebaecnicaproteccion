package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aporte")
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String afiliadoId;
    
    // Representa el monto del aporte en pesos colombianos
    private BigDecimal monto;

    private LocalDate fecha;

    private String canal;

    // Formato YYYY-MM, derivado de la fecha de registro
    private String periodo;

    private boolean marcadaRevision;
}

package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "aporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    // Representa el monto del aporte en pesos colombianos
    private double monto;

    private LocalDate fecha;

    private String canal;

    // Formato YYYY-MM, derivado de la fecha de registro
    private String periodo;

    private boolean marcadaRevision;
}

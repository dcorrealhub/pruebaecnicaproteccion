package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "aporte")
// H-018: @Data en entidad JPA; equals/hashCode sobre id mutable y setters públicos que saltan invariantes.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // H-015: sin NOT NULL ni índice (afiliado_id, periodo) en BD.
    private String afiliadoId;

    // Representa el monto del aporte en pesos colombianos
    // H-005: dinero como double; debe ser BigDecimal con NUMERIC(19,2).
    private double monto;

    private LocalDate fecha;

    private String canal;

    // Formato YYYY-MM, derivado de la fecha de registro
    private String periodo;

    private boolean marcadaRevision;
}

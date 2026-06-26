package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(precision = 19, scale = 2)
    private BigDecimal monto;

    private LocalDate fecha;

    private String canal;

    // Formato YYYY-MM, derivado de la fecha de registro
    private String periodo;

    private boolean marcadaRevision;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 36)
    private String idempotencyKey;
}

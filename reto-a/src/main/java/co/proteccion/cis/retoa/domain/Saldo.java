package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saldo")
// H-018: @Data expone setTotalMes; cualquier código puede alterar el saldo saltándose el tope.
@Data
@NoArgsConstructor
// H-003: sin @Version ni estrategia de bloqueo; actualizaciones concurrentes se pierden.
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // H-015: sin UNIQUE(afiliado_id, mes) ni NOT NULL en BD.
    private String afiliadoId;

    // Acumulado del mes en pesos colombianos
    // H-005: acumulado monetario como double; deriva y puede quedar en Infinity.
    private double totalMes;

    // Formato YYYY-MM
    // H-006: el mes existe pero nunca se usa para buscar el saldo.
    private String mes;
}

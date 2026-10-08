package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_aporte")
// H-018: @Data en entidad JPA.
@Data
@NoArgsConstructor
public class EventoAporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // H-016: no referencia al aporte (aporteId) ni al actor/canal que lo originó.
    private String afiliadoId;

    // H-005: monto como double.
    private double monto;

    private String tipo;

    private LocalDateTime fechaEvento;

    public EventoAporte(Aporte aporte) {
        this.afiliadoId = aporte.getAfiliadoId();
        this.monto = aporte.getMonto();
        // H-016: tipo como string mágico; debería ser un enum.
        this.tipo = "APORTE_REGISTRADO";
        // H-010: LocalDateTime.now() con la zona de la JVM y distinto instante al del aporte.
        this.fechaEvento = LocalDateTime.now();
    }
}

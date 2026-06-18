package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_aporte")
@Data
@NoArgsConstructor
public class EventoAporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    private double monto;

    private String tipo;

    private LocalDateTime fechaEvento;

    public EventoAporte(Aporte aporte) {
        this.afiliadoId = aporte.getAfiliadoId();
        this.monto = aporte.getMonto();
        this.tipo = "APORTE_REGISTRADO";
        this.fechaEvento = LocalDateTime.now();
    }
}

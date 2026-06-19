package co.proteccion.cis.retoa.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento_aporte")
@Getter
@Setter
@NoArgsConstructor
public class EventoAporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String afiliadoId;

    @Column(precision = 19, scale = 2)
    private BigDecimal monto;

    private String tipo;

    private LocalDateTime fechaEvento;

    public EventoAporte(Aporte aporte) {
        this.afiliadoId = aporte.getAfiliadoId();
        this.monto = aporte.getMonto();
        this.tipo = "APORTE_REGISTRADO";
        this.fechaEvento = LocalDateTime.now();
    }
}
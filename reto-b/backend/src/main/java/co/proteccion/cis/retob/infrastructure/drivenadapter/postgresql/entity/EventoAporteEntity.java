package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evento_aporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aporte_id", nullable = false)
    private Long aporteId;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private OffsetDateTime ocurridoEn;

    @PrePersist
    void prePersist() {
        if (ocurridoEn == null) {
            ocurridoEn = OffsetDateTime.now();
        }
    }
}

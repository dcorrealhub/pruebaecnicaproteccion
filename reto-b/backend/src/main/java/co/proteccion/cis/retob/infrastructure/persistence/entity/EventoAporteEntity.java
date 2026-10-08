package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA de trazabilidad de eventos de aporte. Sin Lombok (ver nota en {@link AporteEntity}).
 */
@Entity
@Table(name = "evento_aporte")
public class EventoAporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aporte_id", nullable = false)
    private Long aporteId;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private OffsetDateTime ocurridoEn;

    protected EventoAporteEntity() {
        // requerido por JPA
    }

    public EventoAporteEntity(Long id, Long aporteId, String tipo, OffsetDateTime ocurridoEn) {
        this.id = id;
        this.aporteId = aporteId;
        this.tipo = tipo;
        this.ocurridoEn = ocurridoEn;
    }

    @PrePersist
    void prePersist() {
        if (ocurridoEn == null) {
            ocurridoEn = OffsetDateTime.now();
        }
    }

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public Long getAporteId()           { return aporteId; }
    public void setAporteId(Long v)     { this.aporteId = v; }
    public String getTipo()             { return tipo; }
    public void setTipo(String v)       { this.tipo = v; }
    public OffsetDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(OffsetDateTime v) { this.ocurridoEn = v; }
}

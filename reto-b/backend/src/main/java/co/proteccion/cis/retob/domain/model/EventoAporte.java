package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.model.enums.TipoEventoAporte;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio: evento ocurrido sobre un aporte (ej. su registro).
 * Clase pura de Java — sin anotaciones de framework ni de persistencia.
 */
public final class EventoAporte {

    private final Long id;
    private final Long aporteId;
    private final TipoEventoAporte tipo;
    private final OffsetDateTime ocurridoEn;

    public EventoAporte(Long id, Long aporteId, TipoEventoAporte tipo, OffsetDateTime ocurridoEn) {
        this.id = id;
        this.aporteId = aporteId;
        this.tipo = tipo;
        this.ocurridoEn = ocurridoEn;
    }

    public Long getId()                { return id; }
    public Long getAporteId()          { return aporteId; }
    public TipoEventoAporte getTipo()  { return tipo; }
    public OffsetDateTime getOcurridoEn() { return ocurridoEn; }
}

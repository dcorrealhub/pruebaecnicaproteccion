package co.proteccion.cis.retob.domain.model;

import java.time.OffsetDateTime;

public final class RevisionAporte {

    private final Long id;
    private final Long aporteId;
    private final String revisor;
    private final EstadoAporte decision;
    private final String comentario;
    private final OffsetDateTime ocurridoEn;

    public RevisionAporte(Long id,
                          Long aporteId,
                          String revisor,
                          EstadoAporte decision,
                          String comentario,
                          OffsetDateTime ocurridoEn) {
        this.id = id;
        this.aporteId = aporteId;
        this.revisor = revisor;
        this.decision = decision;
        this.comentario = comentario;
        this.ocurridoEn = ocurridoEn;
    }

    public Long getId()                  { return id; }
    public Long getAporteId()            { return aporteId; }
    public String getRevisor()           { return revisor; }
    public EstadoAporte getDecision()    { return decision; }
    public String getComentario()        { return comentario; }
    public OffsetDateTime getOcurridoEn() { return ocurridoEn; }
}

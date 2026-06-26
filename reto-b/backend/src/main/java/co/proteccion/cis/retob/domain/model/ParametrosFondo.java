package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ParametrosFondo {

    private final String id;
    private final BigDecimal topeMensual;
    private final BigDecimal umbralRevision;
    private final String modificadoPor;
    private final OffsetDateTime modificadoEn;
    private final String comentario;

    public ParametrosFondo(String id,
                           BigDecimal topeMensual,
                           BigDecimal umbralRevision,
                           String modificadoPor,
                           OffsetDateTime modificadoEn,
                           String comentario) {
        this.id = id;
        this.topeMensual = topeMensual;
        this.umbralRevision = umbralRevision;
        this.modificadoPor = modificadoPor;
        this.modificadoEn = modificadoEn;
        this.comentario = comentario;
    }

    public String getId()                   { return id; }
    public BigDecimal getTopeMensual()      { return topeMensual; }
    public BigDecimal getUmbralRevision()   { return umbralRevision; }
    public String getModificadoPor()        { return modificadoPor; }
    public OffsetDateTime getModificadoEn() { return modificadoEn; }
    public String getComentario()           { return comentario; }
}

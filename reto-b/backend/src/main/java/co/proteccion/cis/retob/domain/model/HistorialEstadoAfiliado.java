package co.proteccion.cis.retob.domain.model;

import java.time.OffsetDateTime;

public final class HistorialEstadoAfiliado {

    private final String id;
    private final String afiliadoId;
    private final EstadoAfiliado estadoAnterior;
    private final EstadoAfiliado estadoNuevo;
    private final OffsetDateTime cambiadoEn;

    public HistorialEstadoAfiliado(String id, String afiliadoId,
                                   EstadoAfiliado estadoAnterior,
                                   EstadoAfiliado estadoNuevo,
                                   OffsetDateTime cambiadoEn) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.cambiadoEn = cambiadoEn;
    }

    public String getId()                       { return id; }
    public String getAfiliadoId()               { return afiliadoId; }
    public EstadoAfiliado getEstadoAnterior()   { return estadoAnterior; }
    public EstadoAfiliado getEstadoNuevo()      { return estadoNuevo; }
    public OffsetDateTime getCambiadoEn()       { return cambiadoEn; }
}

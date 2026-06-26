package co.proteccion.cis.retob.domain.model;

import java.time.OffsetDateTime;

public final class Afiliado {

    private final String id;
    private final String afiliadoId;
    private final String nombre;
    private final EstadoAfiliado estado;
    private final OffsetDateTime creadoEn;

    public Afiliado(String id, String afiliadoId, String nombre, EstadoAfiliado estado, OffsetDateTime creadoEn) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.nombre = nombre;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }

    public String getId()                { return id; }
    public String getAfiliadoId()        { return afiliadoId; }
    public String getNombre()            { return nombre; }
    public EstadoAfiliado getEstado()    { return estado; }
    public OffsetDateTime getCreadoEn()  { return creadoEn; }
}

package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Aporte {

    private final String id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final CanalOrigen canal;
    private final String periodo;
    private final EstadoAporte estado;
    private final String idempotenciaKey;

    public Aporte(String id,
                  String afiliadoId,
                  BigDecimal monto,
                  LocalDate fecha,
                  CanalOrigen canal,
                  String periodo,
                  EstadoAporte estado,
                  String idempotenciaKey) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.monto = monto;
        this.fecha = fecha;
        this.canal = canal;
        this.periodo = periodo;
        this.estado = estado;
        this.idempotenciaKey = idempotenciaKey;
    }

    public String getId()                { return id; }
    public String getAfiliadoId()        { return afiliadoId; }
    public BigDecimal getMonto()         { return monto; }
    public LocalDate getFecha()          { return fecha; }
    public CanalOrigen getCanal()        { return canal; }
    public String getPeriodo()           { return periodo; }
    public EstadoAporte getEstado()      { return estado; }
    public String getIdempotenciaKey()   { return idempotenciaKey; }
}

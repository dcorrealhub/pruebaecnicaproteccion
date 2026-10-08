package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad de dominio: representa un aporte a un fondo voluntario.
 * Clase pura de Java — sin anotaciones de framework ni de persistencia.
 */
public final class Aporte {

    private final Long id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final Canal canal;
    private final String periodo;        // formato YYYY-MM
    private final boolean marcadaRevision;
    private final String idempotenciaKey;

    public Aporte(Long id,
                  String afiliadoId,
                  BigDecimal monto,
                  LocalDate fecha,
                  Canal canal,
                  String periodo,
                  boolean marcadaRevision,
                  String idempotenciaKey) {
        this.id = id;
        this.afiliadoId = Objects.requireNonNull(afiliadoId, "afiliadoId");
        this.monto = Objects.requireNonNull(monto, "monto");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.marcadaRevision = marcadaRevision;
        this.idempotenciaKey = Objects.requireNonNull(idempotenciaKey, "idempotenciaKey");
    }

    /**
     * Indica si una nueva solicitud con la misma clave de idempotencia describe
     * exactamente el mismo aporte. Los montos se comparan con {@code compareTo}
     * para que 100 y 100.00 se consideren iguales.
     */
    public boolean mismoContenido(String afiliadoId, BigDecimal monto, Canal canal) {
        return this.afiliadoId.equals(afiliadoId)
                && this.monto.compareTo(monto) == 0
                && this.canal == canal;
    }

    public Long getId()              { return id; }
    public String getAfiliadoId()    { return afiliadoId; }
    public BigDecimal getMonto()     { return monto; }
    public LocalDate getFecha()      { return fecha; }
    public Canal getCanal()          { return canal; }
    public String getPeriodo()       { return periodo; }
    public boolean isMarcadaRevision() { return marcadaRevision; }
    public String getIdempotenciaKey() { return idempotenciaKey; }
}

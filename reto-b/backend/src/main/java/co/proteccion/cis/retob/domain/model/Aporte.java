package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad de dominio: representa un aporte a un fondo voluntario.
 * Clase pura de Java — sin anotaciones de framework ni de persistencia.
 */
public final class Aporte {

    private final Long id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final String canal;
    private final String periodo;        // formato YYYY-MM
    private final boolean marcadaRevision;
    private final String idempotenciaKey;

    // La validación vive en el constructor para que ninguna capa pueda construir
    // un Aporte inválido, sin depender de frameworks externos.
    public Aporte(Long id,
                  String afiliadoId,
                  BigDecimal monto,
                  LocalDate fecha,
                  String canal,
                  String periodo,
                  boolean marcadaRevision,
                  String idempotenciaKey) {
        // Invariante del dominio: un aporte con monto nulo o no positivo no tiene sentido de negocio.
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.monto = monto;
        this.fecha = fecha;
        this.canal = canal;
        this.periodo = periodo;
        this.marcadaRevision = marcadaRevision;
        this.idempotenciaKey = idempotenciaKey;
    }

    public Long getId()              { return id; }
    public String getAfiliadoId()    { return afiliadoId; }
    public BigDecimal getMonto()     { return monto; }
    public LocalDate getFecha()      { return fecha; }
    public String getCanal()         { return canal; }
    public String getPeriodo()       { return periodo; }
    public boolean isMarcadaRevision() { return marcadaRevision; }
    public String getIdempotenciaKey() { return idempotenciaKey; }
}

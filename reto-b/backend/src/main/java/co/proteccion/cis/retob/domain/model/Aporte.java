package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entidad de dominio: representa un aporte a un fondo voluntario.
 * Clase pura de Java — sin anotaciones de framework ni de persistencia.
 *
 * <p>El monto se normaliza siempre a escala 2 con {@link RoundingMode#HALF_EVEN}.
 * El periodo (YYYY-MM) se deriva de la fecha.</p>
 */
public final class Aporte {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Long id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final Canal canal;
    private final String periodo;        // formato YYYY-MM, derivado de fecha
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
        this.afiliadoId = afiliadoId;
        this.monto = monto;
        this.fecha = fecha;
        this.canal = canal;
        this.periodo = periodo;
        this.marcadaRevision = marcadaRevision;
        this.idempotenciaKey = idempotenciaKey;
    }

    /**
     * Crea un aporte nuevo (sin id), validando invariantes y normalizando el monto a escala 2.
     * El periodo se deriva de la fecha.
     *
     * @throws MontoInvalidoException si el monto es nulo o no positivo
     */
    public static Aporte nuevo(String afiliadoId,
                               BigDecimal monto,
                               LocalDate fecha,
                               Canal canal,
                               boolean marcadaRevision,
                               String idempotenciaKey) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException("El monto del aporte debe ser mayor a cero.");
        }
        BigDecimal montoNormalizado = monto.setScale(2, RoundingMode.HALF_EVEN);
        String periodo = fecha.format(PERIODO_FMT);
        return new Aporte(null, afiliadoId, montoNormalizado, fecha, canal,
                periodo, marcadaRevision, idempotenciaKey);
    }

    public Long getId()                { return id; }
    public String getAfiliadoId()      { return afiliadoId; }
    public BigDecimal getMonto()       { return monto; }
    public LocalDate getFecha()        { return fecha; }
    public Canal getCanal()            { return canal; }
    public String getPeriodo()         { return periodo; }
    public boolean isMarcadaRevision() { return marcadaRevision; }
    public String getIdempotenciaKey() { return idempotenciaKey; }
}

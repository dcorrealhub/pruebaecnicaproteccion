package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entidad de dominio: representa un aporte a un fondo voluntario.
 * Clase pura de Java — sin anotaciones de framework ni de persistencia.
 *
 * <p>Es inmutable: las transiciones de estado ({@link #aprobar()}, {@link #rechazar()})
 * devuelven una nueva instancia. El {@code periodo} (YYYY-MM) se deriva de la fecha.
 */
public final class Aporte {

    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Long id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final String canal;
    private final String periodo;        // formato YYYY-MM, derivado de la fecha
    private final EstadoAporte estado;
    private final String idempotenciaKey;

    public Aporte(Long id,
                  String afiliadoId,
                  BigDecimal monto,
                  LocalDate fecha,
                  String canal,
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

    /**
     * Crea un aporte nuevo (aún sin id), validando las invariantes de dominio.
     * El estado lo decide el caso de uso según las reglas de negocio (umbral).
     *
     * @throws IllegalArgumentException si el monto no es positivo o faltan datos obligatorios
     */
    public static Aporte nuevo(String afiliadoId,
                               BigDecimal monto,
                               LocalDate fecha,
                               String canal,
                               EstadoAporte estado,
                               String idempotenciaKey) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        exigirTexto(afiliadoId, "El afiliadoId es obligatorio");
        exigirTexto(canal, "El canal es obligatorio");
        exigirTexto(idempotenciaKey, "La clave de idempotencia es obligatoria");
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        return new Aporte(null, afiliadoId, monto, fecha, canal,
                fecha.format(PERIODO_FMT), estado, idempotenciaKey);
    }

    /** Aprueba un aporte pendiente. */
    public Aporte aprobar() {
        if (estado != EstadoAporte.PENDIENTE_REVISION) {
            throw new ReglaNegocioException(
                    "Solo se puede aprobar un aporte en estado PENDIENTE_REVISION (actual: " + estado + ")");
        }
        return conEstado(EstadoAporte.APROBADO);
    }

    /** Rechaza un aporte pendiente. */
    public Aporte rechazar() {
        if (estado != EstadoAporte.PENDIENTE_REVISION) {
            throw new ReglaNegocioException(
                    "Solo se puede rechazar un aporte en estado PENDIENTE_REVISION (actual: " + estado + ")");
        }
        return conEstado(EstadoAporte.RECHAZADO);
    }

    private Aporte conEstado(EstadoAporte nuevoEstado) {
        return new Aporte(id, afiliadoId, monto, fecha, canal, periodo, nuevoEstado, idempotenciaKey);
    }

    private static void exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    public Long getId()                { return id; }
    public String getAfiliadoId()      { return afiliadoId; }
    public BigDecimal getMonto()       { return monto; }
    public LocalDate getFecha()        { return fecha; }
    public String getCanal()           { return canal; }
    public String getPeriodo()         { return periodo; }
    public EstadoAporte getEstado()    { return estado; }
    public boolean isMarcadaRevision() { return estado == EstadoAporte.PENDIENTE_REVISION; }
    public String getIdempotenciaKey() { return idempotenciaKey; }
}

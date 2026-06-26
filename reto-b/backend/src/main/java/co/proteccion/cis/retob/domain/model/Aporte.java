package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class Aporte {

    private final Long id;
    private final String afiliadoId;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final String canal;
    private final String periodo;
    private final boolean marcadaRevision;
    private final String idempotenciaKey;

    private Aporte(Long id,
                   String afiliadoId,
                   BigDecimal monto,
                   LocalDate fecha,
                   String canal,
                   boolean marcadaRevision,
                   String idempotenciaKey) {

        this.afiliadoId      = requireNonBlank(afiliadoId, "afiliadoId no puede ser vacío");
        this.monto           = requirePositive(monto, "El monto debe ser positivo");
        this.fecha           = Objects.requireNonNull(fecha, "La fecha es obligatoria");
        this.canal           = requireNonBlank(canal, "El canal no puede ser vacío");
        this.idempotenciaKey = requireNonBlank(idempotenciaKey, "La clave de idempotencia es obligatoria");
        this.id              = id;
        this.marcadaRevision = marcadaRevision;
        this.periodo         = YearMonth.from(fecha).toString();
    }

    // Para crear un aporte nuevo (sin id)
    public static Aporte nuevo(String afiliadoId,
                               BigDecimal monto,
                               LocalDate fecha,
                               String canal,
                               boolean marcadaRevision,
                               String idempotenciaKey) {
        return new Aporte(null, afiliadoId, monto, fecha, canal, marcadaRevision, idempotenciaKey);
    }

    // Para reconstruir desde base de datos (con id)
    public static Aporte reconstruir(Long id,
                                     String afiliadoId,
                                     BigDecimal monto,
                                     LocalDate fecha,
                                     String canal,
                                     boolean marcadaRevision,
                                     String idempotenciaKey) {
        return new Aporte(id, afiliadoId, monto, fecha, canal, marcadaRevision, idempotenciaKey);
    }

    public Long getId()                { return id; }
    public String getAfiliadoId()      { return afiliadoId; }
    public BigDecimal getMonto()       { return monto; }
    public LocalDate getFecha()        { return fecha; }
    public String getCanal()           { return canal; }
    public String getPeriodo()         { return periodo; }
    public boolean isMarcadaRevision() { return marcadaRevision; }
    public String getIdempotenciaKey() { return idempotenciaKey; }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException(message);
        return value;
    }
}

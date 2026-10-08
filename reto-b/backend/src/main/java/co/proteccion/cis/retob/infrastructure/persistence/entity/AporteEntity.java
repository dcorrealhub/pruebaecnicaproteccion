package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidad JPA de un aporte. Sin Lombok: la generación de código de Lombok no es fiable
 * bajo el toolchain JDK 26 de este entorno, por lo que accesores y constructores son explícitos.
 */
@Entity
@Table(name = "aporte")
public class AporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    private String canal;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(name = "marcada_revision", nullable = false)
    private boolean marcadaRevision;

    @Column(name = "idempotencia_key", nullable = false, unique = true, length = 100)
    private String idempotenciaKey;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected AporteEntity() {
        // requerido por JPA
    }

    public AporteEntity(Long id, String afiliadoId, BigDecimal monto, LocalDate fecha,
                        String canal, String periodo, boolean marcadaRevision,
                        String idempotenciaKey, OffsetDateTime creadoEn) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.monto = monto;
        this.fecha = fecha;
        this.canal = canal;
        this.periodo = periodo;
        this.marcadaRevision = marcadaRevision;
        this.idempotenciaKey = idempotenciaKey;
        this.creadoEn = creadoEn;
    }

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public String getAfiliadoId()       { return afiliadoId; }
    public void setAfiliadoId(String v) { this.afiliadoId = v; }
    public BigDecimal getMonto()        { return monto; }
    public void setMonto(BigDecimal v)  { this.monto = v; }
    public LocalDate getFecha()         { return fecha; }
    public void setFecha(LocalDate v)   { this.fecha = v; }
    public String getCanal()            { return canal; }
    public void setCanal(String v)      { this.canal = v; }
    public String getPeriodo()          { return periodo; }
    public void setPeriodo(String v)    { this.periodo = v; }
    public boolean isMarcadaRevision()  { return marcadaRevision; }
    public void setMarcadaRevision(boolean v) { this.marcadaRevision = v; }
    public String getIdempotenciaKey()  { return idempotenciaKey; }
    public void setIdempotenciaKey(String v)  { this.idempotenciaKey = v; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime v) { this.creadoEn = v; }
}

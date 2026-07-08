package co.proteccion.cis.retob.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aportes", indexes = {
        @Index(name = "idx_aportes_afiliado_periodo", columnList = "afiliadoId,periodo"),
        @Index(name = "idx_aportes_idempotencia", columnList = "idempotenciaKey", unique = true)
})
public class AporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    private String canal;

    @Column(nullable = false, length = 7)
    private String periodo; // YYYY-MM

    @Column(nullable = false)
    private boolean marcadaRevision;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotenciaKey;

    public AporteEntity() {}

    public AporteEntity(Long id, String afiliadoId, BigDecimal monto, LocalDate fecha, String canal, String periodo, boolean marcadaRevision, String idempotenciaKey) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.monto = monto;
        this.fecha = fecha;
        this.canal = canal;
        this.periodo = periodo;
        this.marcadaRevision = marcadaRevision;
        this.idempotenciaKey = idempotenciaKey;
    }

    public static AporteEntity builder() {
        return new AporteEntity();
    }

    public AporteEntity id(Long id) { this.id = id; return this; }
    public AporteEntity afiliadoId(String afiliadoId) { this.afiliadoId = afiliadoId; return this; }
    public AporteEntity monto(BigDecimal monto) { this.monto = monto; return this; }
    public AporteEntity fecha(LocalDate fecha) { this.fecha = fecha; return this; }
    public AporteEntity canal(String canal) { this.canal = canal; return this; }
    public AporteEntity periodo(String periodo) { this.periodo = periodo; return this; }
    public AporteEntity marcadaRevision(boolean marcadaRevision) { this.marcadaRevision = marcadaRevision; return this; }
    public AporteEntity idempotenciaKey(String idempotenciaKey) { this.idempotenciaKey = idempotenciaKey; return this; }

    public Long getId() { return id; }
    public String getAfiliadoId() { return afiliadoId; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public String getCanal() { return canal; }
    public String getPeriodo() { return periodo; }
    public boolean isMarcadaRevision() { return marcadaRevision; }
    public String getIdempotenciaKey() { return idempotenciaKey; }
}

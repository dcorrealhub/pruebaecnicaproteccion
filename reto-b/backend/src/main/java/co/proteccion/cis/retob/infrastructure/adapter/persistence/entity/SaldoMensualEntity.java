package co.proteccion.cis.retob.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "saldos_mensuales", indexes = {
        @Index(name = "idx_saldos_afiliado_mes", columnList = "afiliadoId,mes", unique = true)
})
public class SaldoMensualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, length = 7)
    private String mes; // YYYY-MM

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Version
    private Integer version;

    public SaldoMensualEntity() {}

    public SaldoMensualEntity(Long id, String afiliadoId, String mes, BigDecimal total, Integer version) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.mes = mes;
        this.total = total;
        this.version = version;
    }

    public static SaldoMensualEntity builder() {
        return new SaldoMensualEntity();
    }

    public SaldoMensualEntity id(Long id) { this.id = id; return this; }
    public SaldoMensualEntity afiliadoId(String afiliadoId) { this.afiliadoId = afiliadoId; return this; }
    public SaldoMensualEntity mes(String mes) { this.mes = mes; return this; }
    public SaldoMensualEntity total(BigDecimal total) { this.total = total; return this; }
    public SaldoMensualEntity version(Integer version) { this.version = version; return this; }

    public Long getId() { return id; }
    public String getAfiliadoId() { return afiliadoId; }
    public String getMes() { return mes; }
    public BigDecimal getTotal() { return total; }
    public Integer getVersion() { return version; }
}

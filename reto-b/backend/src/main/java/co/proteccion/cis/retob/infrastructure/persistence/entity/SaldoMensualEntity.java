package co.proteccion.cis.retob.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Entidad JPA del acumulado mensual por afiliado. Sin Lombok (ver nota en {@link AporteEntity}).
 * El campo {@code version} con {@code @Version} habilita el bloqueo optimista.
 */
@Entity
@Table(name = "saldo_mensual",
       uniqueConstraints = @UniqueConstraint(columnNames = {"afiliado_id", "mes"}))
public class SaldoMensualEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false, length = 50)
    private String afiliadoId;

    @Column(nullable = false, length = 7)
    private String mes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Version
    @Column(nullable = false)
    private Integer version;

    protected SaldoMensualEntity() {
        // requerido por JPA
    }

    public SaldoMensualEntity(Long id, String afiliadoId, String mes,
                              BigDecimal total, Integer version) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.mes = mes;
        this.total = total;
        this.version = version;
    }

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public String getAfiliadoId()       { return afiliadoId; }
    public void setAfiliadoId(String v) { this.afiliadoId = v; }
    public String getMes()              { return mes; }
    public void setMes(String v)        { this.mes = v; }
    public BigDecimal getTotal()        { return total; }
    public void setTotal(BigDecimal v)  { this.total = v; }
    public Integer getVersion()         { return version; }
    public void setVersion(Integer v)   { this.version = v; }
}

package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;

/**
 * Entidad de dominio: acumulado mensual de aportes por afiliado.
 * El campo {@code version} soporta control de concurrencia optimista.
 */
public final class SaldoMensual {

    private final Long id;
    private final String afiliadoId;
    private final String mes;       // formato YYYY-MM
    private BigDecimal total;
    private final Integer version;  // control de concurrencia optimista

    public SaldoMensual(Long id, String afiliadoId, String mes, BigDecimal total, Integer version) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.mes = mes;
        this.total = total;
        this.version = version;
    }

    public BigDecimal calcularNuevoTotal(BigDecimal monto) {
        return this.total.add(monto);
    }

    public SaldoMensual conTotal(BigDecimal nuevoTotal) {
        return new SaldoMensual(this.id, this.afiliadoId, this.mes, nuevoTotal, this.version);
    }

    public Long getId()           { return id; }
    public String getAfiliadoId() { return afiliadoId; }
    public String getMes()        { return mes; }
    public BigDecimal getTotal()  { return total; }
    public Integer getVersion()   { return version; }
}

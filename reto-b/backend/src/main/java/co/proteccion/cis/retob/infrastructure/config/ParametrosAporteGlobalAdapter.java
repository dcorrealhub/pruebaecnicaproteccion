package co.proteccion.cis.retob.infrastructure.config;

import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.port.out.ParametrosAportePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resolución global de parámetros de aporte: lee de configuración el tope mensual, el umbral de
 * revisión por defecto y el umbral específico del canal SUCURSAL (mayor riesgo → umbral más bajo).
 *
 * <p>Claves ({@code application.properties}):</p>
 * <ul>
 *   <li>{@code aporte.tope-mensual}</li>
 *   <li>{@code aporte.umbral-revision} — umbral por defecto para todos los canales</li>
 *   <li>{@code aporte.umbral-revision-sucursal} — override para canal SUCURSAL</li>
 * </ul>
 *
 * <p>Hoy los valores son globales (iguales para todo afiliado y mes). Para pasar a parámetros por
 * afiliado basta sustituir esta implementación por otra que consulte una tabla/servicio, sin tocar
 * el caso de uso ni el dominio (la firma ya recibe afiliadoId y mes).</p>
 */
@Component
public class ParametrosAporteGlobalAdapter implements ParametrosAportePort {

    private final BigDecimal topeMensual;
    private final BigDecimal umbralRevisionDefault;
    private final BigDecimal umbralRevisionSucursal;

    public ParametrosAporteGlobalAdapter(
            @Value("${aporte.tope-mensual:10000000}") BigDecimal topeMensual,
            @Value("${aporte.umbral-revision:5000000}") BigDecimal umbralRevisionDefault,
            @Value("${aporte.umbral-revision-sucursal:3000000}") BigDecimal umbralRevisionSucursal) {
        this.topeMensual = topeMensual;
        this.umbralRevisionDefault = umbralRevisionDefault;
        this.umbralRevisionSucursal = umbralRevisionSucursal;
    }

    @Override
    public ParametrosAporte resolver(String afiliadoId, String mes) {
        return new ParametrosAporte(
                topeMensual,
                umbralRevisionDefault,
                Map.of(Canal.SUCURSAL, umbralRevisionSucursal));
    }
}

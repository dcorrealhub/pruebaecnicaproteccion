package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.exception.SolicitudInvalidaException;

import java.util.Arrays;

/**
 * Canales de origen admitidos para un aporte.
 * Lista cerrada: un canal desconocido se rechaza en lugar de persistirse como texto libre.
 */
public enum Canal {
    APP_MOVIL,
    WEB,
    SUCURSAL;

    public static Canal desde(String valor) {
        return Arrays.stream(values())
                .filter(c -> c.name().equals(valor))
                .findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "CANAL_INVALIDO",
                        "Canal no soportado. Valores permitidos: APP_MOVIL, WEB, SUCURSAL"));
    }
}

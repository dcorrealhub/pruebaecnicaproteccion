package co.proteccion.cis.retob.domain.model;

/**
 * Canal de origen de un aporte voluntario.
 * Enum de dominio: la validación del borde (Bean Validation) debe mapear a estos valores.
 */
public enum Canal {
    APP_MOVIL,
    WEB,
    SUCURSAL
}

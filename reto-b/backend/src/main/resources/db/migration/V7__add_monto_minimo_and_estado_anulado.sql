-- V7: monto_minimo en parámetros del fondo
-- El estado en aporte es VARCHAR(20), ANULADO no requiere DDL adicional.

ALTER TABLE historico_parametros
    ADD COLUMN monto_minimo NUMERIC(15, 2) NOT NULL DEFAULT 10000.00;

-- Quitar el default para que las inserciones futuras sean explícitas
ALTER TABLE historico_parametros
    ALTER COLUMN monto_minimo DROP DEFAULT;

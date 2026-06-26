-- V2: parámetros configurables en BD + estado del aporte
--
-- Decisión de diseño:
--   * El tope mensual y el umbral de revisión dejan de vivir en application.properties
--     y pasan a ser configurables en runtime desde esta tabla.
--   * La fila con afiliado_id IS NULL es el valor GLOBAL por defecto; filas con
--     afiliado_id no nulo sobreescriben el valor para ese afiliado puntual.
CREATE TABLE IF NOT EXISTS parametro_aporte (
    id              BIGSERIAL PRIMARY KEY,
    afiliado_id     VARCHAR(50),                  -- NULL = parámetros globales por defecto
    tope_mensual    NUMERIC(15, 2) NOT NULL,
    umbral_revision NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uq_parametro_afiliado UNIQUE (afiliado_id),
    CONSTRAINT ck_parametro_positivo CHECK (tope_mensual > 0 AND umbral_revision > 0)
);

-- Valores por defecto (equivalentes a los que antes vivían en application.properties)
INSERT INTO parametro_aporte (afiliado_id, tope_mensual, umbral_revision)
VALUES (NULL, 10000000, 5000000);

-- Estado del ciclo de vida del aporte.
--   APROBADO          -> cuenta para el tope mensual (saldo_mensual)
--   PENDIENTE_REVISION-> superó el umbral; NO cuenta para el tope hasta ser aprobado
--   RECHAZADO         -> no cuenta para el tope
-- Los aportes existentes se asumen aprobados; los nuevos definen su estado en el dominio.
ALTER TABLE aporte ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'APROBADO';

CREATE INDEX IF NOT EXISTS idx_aporte_afiliado_estado ON aporte (afiliado_id, estado);

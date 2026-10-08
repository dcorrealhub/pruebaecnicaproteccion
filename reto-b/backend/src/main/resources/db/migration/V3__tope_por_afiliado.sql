-- V3: tope mensual configurable por afiliado.
-- Si un afiliado no tiene fila aquí, aplica el tope por defecto (aporte.tope-mensual).

CREATE TABLE IF NOT EXISTS tope_afiliado (
    afiliado_id    VARCHAR(50)    PRIMARY KEY,
    tope_mensual   NUMERIC(15, 2) NOT NULL,
    actualizado_en TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tope_afiliado_positivo CHECK (tope_mensual > 0)
);

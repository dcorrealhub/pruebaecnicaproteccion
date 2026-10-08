-- V4: el umbral de revisión también es configurable por afiliado.
-- La tabla pasa a agrupar todos los parámetros de un afiliado (se conservan los topes ya cargados).
-- Cada columna es opcional: si es NULL, aplica el valor por defecto de la configuración
-- (aporte.tope-mensual / aporte.umbral-revision).

ALTER TABLE tope_afiliado RENAME TO parametro_afiliado;
ALTER TABLE parametro_afiliado RENAME CONSTRAINT ck_tope_afiliado_positivo TO ck_parametro_tope_positivo;
ALTER TABLE parametro_afiliado ALTER COLUMN tope_mensual DROP NOT NULL;

ALTER TABLE parametro_afiliado
    ADD COLUMN umbral_revision NUMERIC(15, 2),
    ADD CONSTRAINT ck_parametro_umbral_positivo CHECK (umbral_revision > 0);

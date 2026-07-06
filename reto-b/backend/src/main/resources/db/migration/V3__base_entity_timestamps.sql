-- V3: alinear el esquema con la refactorización a arquitectura hexagonal (estilo skill).
--   * BaseEntity aporta created_at / updated_at a las entidades de agregado.
--   * El estado del aporte reemplaza al booleano marcada_revision (se deriva del estado).
--   * created_at reemplaza a la marca creado_en propia del aporte.

ALTER TABLE aporte DROP COLUMN IF EXISTS marcada_revision;
ALTER TABLE aporte DROP COLUMN IF EXISTS creado_en;
ALTER TABLE aporte ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE aporte ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE saldo_mensual ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE saldo_mensual ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE parametro_aporte ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE parametro_aporte ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

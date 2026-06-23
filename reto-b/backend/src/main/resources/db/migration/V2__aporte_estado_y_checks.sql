-- Reemplaza marcada_revision (boolean) por estado (enum persistido como VARCHAR)
-- y agrega constraints de integridad referenciados por las entidades JPA.

ALTER TABLE aporte
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30);

UPDATE aporte
SET estado = CASE
    WHEN marcada_revision = TRUE THEN 'REQUIERE_REVISION'
    ELSE 'REGISTRADO'
END
WHERE estado IS NULL;

ALTER TABLE aporte
    ALTER COLUMN estado SET NOT NULL;

ALTER TABLE aporte
    DROP COLUMN IF EXISTS marcada_revision;

ALTER TABLE aporte
    DROP CONSTRAINT IF EXISTS chk_aporte_monto_positivo;

ALTER TABLE aporte
    ADD CONSTRAINT chk_aporte_monto_positivo CHECK (monto > 0);

ALTER TABLE saldo_mensual
    DROP CONSTRAINT IF EXISTS chk_saldo_total_no_negativo;

ALTER TABLE saldo_mensual
    ADD CONSTRAINT chk_saldo_total_no_negativo CHECK (total >= 0);

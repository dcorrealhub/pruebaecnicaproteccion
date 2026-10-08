-- V2: corrección de precisión monetaria y constraint de dominio en BD.
-- AGENTS.md: dinero en NUMERIC(19,2) y CHECK (monto > 0).

-- Ampliar precisión de columnas monetarias de (15,2) a (19,2).
ALTER TABLE aporte        ALTER COLUMN monto TYPE NUMERIC(19, 2);
ALTER TABLE saldo_mensual ALTER COLUMN total TYPE NUMERIC(19, 2);

-- Invariante de dominio reforzado en la base: el monto siempre es positivo.
ALTER TABLE aporte ADD CONSTRAINT chk_aporte_monto_positivo CHECK (monto > 0);

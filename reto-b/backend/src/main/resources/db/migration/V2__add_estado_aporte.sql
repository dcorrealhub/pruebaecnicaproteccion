-- Reemplaza el boolean marcada_revision por el enum de estado
ALTER TABLE aporte DROP COLUMN marcada_revision;
ALTER TABLE aporte ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';

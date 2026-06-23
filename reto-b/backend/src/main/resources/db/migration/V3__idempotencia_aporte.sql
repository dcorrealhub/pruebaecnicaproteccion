-- Tabla dedicada de idempotencia: claim atómico + respuesta original por clave

CREATE TABLE IF NOT EXISTS idempotencia_aporte (
    id               BIGSERIAL PRIMARY KEY,
    idempotencia_key VARCHAR(100) NOT NULL,
    aporte_id        BIGINT REFERENCES aporte (id),
    estado           VARCHAR(20)  NOT NULL DEFAULT 'EN_PROCESO',
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotencia_aporte_key UNIQUE (idempotencia_key),
    CONSTRAINT chk_idempotencia_estado CHECK (estado IN ('EN_PROCESO', 'COMPLETADO'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_idempotencia_aporte_key ON idempotencia_aporte (idempotencia_key);

-- Migrar claves ya persistidas en aporte (instalaciones existentes)
INSERT INTO idempotencia_aporte (idempotencia_key, aporte_id, estado)
SELECT a.idempotencia_key, a.id, 'COMPLETADO'
FROM aporte a
ON CONFLICT (idempotencia_key) DO NOTHING;

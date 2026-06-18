-- Esquema inicial — aportes voluntarios
-- V1: estructura base

CREATE TABLE IF NOT EXISTS saldo_mensual (
    id          BIGSERIAL PRIMARY KEY,
    afiliado_id VARCHAR(50)    NOT NULL,
    mes         VARCHAR(7)     NOT NULL,           -- formato YYYY-MM
    total       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    version     INTEGER        NOT NULL DEFAULT 0, -- control de concurrencia optimista
    CONSTRAINT uq_saldo_afiliado_mes UNIQUE (afiliado_id, mes)
);

CREATE TABLE IF NOT EXISTS aporte (
    id               BIGSERIAL PRIMARY KEY,
    afiliado_id      VARCHAR(50)    NOT NULL,
    monto            NUMERIC(15, 2) NOT NULL,
    fecha            DATE           NOT NULL,
    canal            VARCHAR(50)    NOT NULL,
    periodo          VARCHAR(7)     NOT NULL,      -- formato YYYY-MM
    marcada_revision BOOLEAN        NOT NULL DEFAULT FALSE,
    idempotencia_key VARCHAR(100)   NOT NULL,
    creado_en        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_aporte_idempotencia UNIQUE (idempotencia_key)
);

CREATE TABLE IF NOT EXISTS evento_aporte (
    id          BIGSERIAL PRIMARY KEY,
    aporte_id   BIGINT      NOT NULL REFERENCES aporte (id),
    tipo        VARCHAR(50) NOT NULL,              -- APORTE_REGISTRADO, APORTE_REVERSADO
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_aporte_afiliado_periodo ON aporte (afiliado_id, periodo);
CREATE INDEX IF NOT EXISTS idx_evento_aporte_id ON evento_aporte (aporte_id);

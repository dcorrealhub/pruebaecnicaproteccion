CREATE TABLE afiliado (
    id          BIGSERIAL    PRIMARY KEY,
    afiliado_id VARCHAR(50)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_afiliado_id ON afiliado (afiliado_id);

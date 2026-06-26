CREATE TABLE revision_aporte (
    id          BIGSERIAL    PRIMARY KEY,
    aporte_id   BIGINT       NOT NULL REFERENCES aporte (id),
    revisor     VARCHAR(100) NOT NULL,
    decision    VARCHAR(20)  NOT NULL,
    comentario  TEXT,
    ocurrido_en TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revision_aporte_id ON revision_aporte (aporte_id);

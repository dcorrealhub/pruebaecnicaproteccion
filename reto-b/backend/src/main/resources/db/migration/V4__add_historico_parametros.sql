CREATE TABLE historico_parametros (
    id              BIGSERIAL      PRIMARY KEY,
    tope_mensual    NUMERIC(15, 2) NOT NULL,
    umbral_revision NUMERIC(15, 2) NOT NULL,
    modificado_por  VARCHAR(100)   NOT NULL,
    modificado_en   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    comentario      TEXT
);

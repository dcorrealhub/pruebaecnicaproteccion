-- V6: Migrar claves primarias de BIGSERIAL a UUID
-- Estrategia: agregar columnas UUID, poblar, reemplazar PKs y FKs manteniendo los datos.
-- Requiere PostgreSQL 13+ (gen_random_uuid() nativo, sin extensión).

-- ─── 1. Agregar columnas UUID a todas las tablas ─────────────────────────────
ALTER TABLE aporte               ADD COLUMN _id UUID DEFAULT gen_random_uuid();
ALTER TABLE afiliado             ADD COLUMN _id UUID DEFAULT gen_random_uuid();
ALTER TABLE historico_parametros ADD COLUMN _id UUID DEFAULT gen_random_uuid();
ALTER TABLE revision_aporte      ADD COLUMN _id UUID DEFAULT gen_random_uuid();
ALTER TABLE evento_aporte        ADD COLUMN _id UUID DEFAULT gen_random_uuid();

-- ─── 2. Agregar columnas FK UUID en tablas hijas ─────────────────────────────
ALTER TABLE revision_aporte ADD COLUMN _aporte_id UUID;
ALTER TABLE evento_aporte   ADD COLUMN _aporte_id UUID;

-- ─── 3. Poblar FKs UUID con los UUIDs generados para aporte ─────────────────
UPDATE revision_aporte r SET _aporte_id = a._id FROM aporte a WHERE r.aporte_id = a.id;
UPDATE evento_aporte   ev SET _aporte_id = a._id FROM aporte a WHERE ev.aporte_id = a.id;

-- ─── 4. Eliminar índice que depende de la FK antigua ─────────────────────────
DROP INDEX IF EXISTS idx_revision_aporte_id;

-- ─── 5. Eliminar FK constraints antes de tocar las PKs ───────────────────────
ALTER TABLE revision_aporte DROP CONSTRAINT IF EXISTS revision_aporte_aporte_id_fkey;
ALTER TABLE evento_aporte   DROP CONSTRAINT IF EXISTS evento_aporte_aporte_id_fkey;

-- ─── 6. Eliminar PKs antiguas ────────────────────────────────────────────────
ALTER TABLE aporte               DROP CONSTRAINT aporte_pkey;
ALTER TABLE afiliado             DROP CONSTRAINT afiliado_pkey;
ALTER TABLE historico_parametros DROP CONSTRAINT historico_parametros_pkey;
ALTER TABLE revision_aporte      DROP CONSTRAINT revision_aporte_pkey;
ALTER TABLE evento_aporte        DROP CONSTRAINT evento_aporte_pkey;

-- ─── 7. Eliminar columnas BIGINT antiguas ────────────────────────────────────
ALTER TABLE aporte               DROP COLUMN id;
ALTER TABLE afiliado             DROP COLUMN id;
ALTER TABLE historico_parametros DROP COLUMN id;
ALTER TABLE revision_aporte      DROP COLUMN id;
ALTER TABLE revision_aporte      DROP COLUMN aporte_id;
ALTER TABLE evento_aporte        DROP COLUMN id;
ALTER TABLE evento_aporte        DROP COLUMN aporte_id;

-- ─── 8. Renombrar columnas UUID ──────────────────────────────────────────────
ALTER TABLE aporte               RENAME COLUMN _id TO id;
ALTER TABLE afiliado             RENAME COLUMN _id TO id;
ALTER TABLE historico_parametros RENAME COLUMN _id TO id;
ALTER TABLE revision_aporte      RENAME COLUMN _id TO id;
ALTER TABLE revision_aporte      RENAME COLUMN _aporte_id TO aporte_id;
ALTER TABLE evento_aporte        RENAME COLUMN _id TO id;
ALTER TABLE evento_aporte        RENAME COLUMN _aporte_id TO aporte_id;

-- ─── 9. Establecer NOT NULL en todas las columnas id ────────────────────────
ALTER TABLE aporte               ALTER COLUMN id SET NOT NULL;
ALTER TABLE afiliado             ALTER COLUMN id SET NOT NULL;
ALTER TABLE historico_parametros ALTER COLUMN id SET NOT NULL;
ALTER TABLE revision_aporte      ALTER COLUMN id SET NOT NULL;
ALTER TABLE revision_aporte      ALTER COLUMN aporte_id SET NOT NULL;
ALTER TABLE evento_aporte        ALTER COLUMN id SET NOT NULL;
ALTER TABLE evento_aporte        ALTER COLUMN aporte_id SET NOT NULL;

-- ─── 10. Recrear PKs ─────────────────────────────────────────────────────────
ALTER TABLE aporte               ADD PRIMARY KEY (id);
ALTER TABLE afiliado             ADD PRIMARY KEY (id);
ALTER TABLE historico_parametros ADD PRIMARY KEY (id);
ALTER TABLE revision_aporte      ADD PRIMARY KEY (id);
ALTER TABLE evento_aporte        ADD PRIMARY KEY (id);

-- ─── 11. Recrear FKs con tipo UUID ───────────────────────────────────────────
ALTER TABLE revision_aporte ADD CONSTRAINT revision_aporte_aporte_id_fkey
    FOREIGN KEY (aporte_id) REFERENCES aporte(id);
ALTER TABLE evento_aporte ADD CONSTRAINT evento_aporte_aporte_id_fkey
    FOREIGN KEY (aporte_id) REFERENCES aporte(id);

-- ─── 12. Recrear índice en la FK UUID ────────────────────────────────────────
CREATE INDEX idx_revision_aporte_id ON revision_aporte(aporte_id);

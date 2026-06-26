-- V8: Triggers de integridad + auditoría de estado del afiliado + vista de resumen
-- ─────────────────────────────────────────────────────────────────────────────────
-- FILOSOFÍA: Los triggers son una segunda capa de defensa (belt-and-suspenders).
--   La aplicación valida primero; la DB garantiza la integridad incluso ante acceso
--   directo por consola, scripts de migración de datos o bugs del application layer.
-- ─────────────────────────────────────────────────────────────────────────────────

-- ── 1. Tabla de auditoría de cambios de estado del afiliado ──────────────────────
-- Poblada automáticamente por el trigger trg_afiliado_historial_estado.
-- La aplicación solo lee de esta tabla; nunca escribe directamente.

CREATE TABLE historial_estado_afiliado (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    afiliado_id VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo    VARCHAR(20) NOT NULL,
    cambiado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_historial_afiliado
        FOREIGN KEY (afiliado_id) REFERENCES afiliado(afiliado_id)
);

CREATE INDEX idx_historial_estado_afiliado_id ON historial_estado_afiliado(afiliado_id);

-- ── 2. TRIGGER: auditoría automática de cambios de estado en afiliado ────────────
-- Registra QUIÉN cambió el estado de un afiliado y CUÁNDO, sin requerir que el
-- application layer lo haga explícitamente. Fuente única de verdad para compliance.

CREATE OR REPLACE FUNCTION fn_afiliado_historial_estado()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado IS DISTINCT FROM NEW.estado THEN
        INSERT INTO historial_estado_afiliado (afiliado_id, estado_anterior, estado_nuevo)
        VALUES (NEW.afiliado_id, OLD.estado, NEW.estado);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_afiliado_historial_estado
    AFTER UPDATE OF estado ON afiliado
    FOR EACH ROW EXECUTE FUNCTION fn_afiliado_historial_estado();

-- ── 3. TRIGGER: validar transición de estado del aporte ─────────────────────────
-- Refuerza la máquina de estados del dominio directamente en la DB.
-- Previene manipulación directa de datos que viole el ciclo de vida del aporte.
-- Transiciones válidas (espejo de EstadoAporte.java):
--   PENDIENTE   → EN_REVISION | APROBADO | ANULADO
--   EN_REVISION → APROBADO   | RECHAZADO

CREATE OR REPLACE FUNCTION fn_validar_transicion_estado_aporte()
RETURNS TRIGGER AS $$
BEGIN
    -- Si el estado no cambia, no hay nada que validar
    IF OLD.estado = NEW.estado THEN
        RETURN NEW;
    END IF;

    IF NOT (
        (OLD.estado = 'PENDIENTE'   AND NEW.estado IN ('EN_REVISION', 'APROBADO', 'ANULADO')) OR
        (OLD.estado = 'EN_REVISION' AND NEW.estado IN ('APROBADO', 'RECHAZADO'))
    ) THEN
        RAISE EXCEPTION 'Transición de estado inválida en aporte: [%] → [%]. '
            'El cambio fue bloqueado a nivel de base de datos.',
            OLD.estado, NEW.estado
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aporte_validar_transicion
    BEFORE UPDATE OF estado ON aporte
    FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_estado_aporte();

-- ── 4. TRIGGER: verificar que el afiliado esté ACTIVO al registrar un aporte ─────
-- Refuerza la regla R2 a nivel de DB.
-- Protege contra inserts directos que bypaseen el application layer.

CREATE OR REPLACE FUNCTION fn_aporte_validar_afiliado_activo()
RETURNS TRIGGER AS $$
DECLARE
    v_estado VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado
    FROM afiliado
    WHERE afiliado_id = NEW.afiliado_id;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'Afiliado [%] no existe en el sistema.', NEW.afiliado_id
            USING ERRCODE = 'P0002';
    END IF;

    IF v_estado <> 'ACTIVO' THEN
        RAISE EXCEPTION 'Afiliado [%] está en estado [%] y no puede registrar aportes.',
            NEW.afiliado_id, v_estado
            USING ERRCODE = 'P0003';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aporte_afiliado_activo
    BEFORE INSERT ON aporte
    FOR EACH ROW EXECUTE FUNCTION fn_aporte_validar_afiliado_activo();

-- ── 5. TRIGGER: invariante de parámetros del fondo ───────────────────────────────
-- Garantiza que montoMinimo < umbralRevision < topeMensual a nivel de DB.
-- Refuerza la regla R14. Previene configuraciones incoherentes vía SQL directo.

CREATE OR REPLACE FUNCTION fn_parametros_validar_invariante()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.monto_minimo >= NEW.umbral_revision THEN
        RAISE EXCEPTION 'Invariante violada: monto_minimo (%) debe ser menor al umbral_revision (%).',
            NEW.monto_minimo, NEW.umbral_revision
            USING ERRCODE = 'P0004';
    END IF;

    IF NEW.umbral_revision >= NEW.tope_mensual THEN
        RAISE EXCEPTION 'Invariante violada: umbral_revision (%) debe ser menor al tope_mensual (%).',
            NEW.umbral_revision, NEW.tope_mensual
            USING ERRCODE = 'P0005';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_parametros_invariante
    BEFORE INSERT ON historico_parametros
    FOR EACH ROW EXECUTE FUNCTION fn_parametros_validar_invariante();

-- ── 6. VISTA: resumen de aportes por afiliado y periodo ──────────────────────────
-- Facilita reportes operativos sin requerir consultas complejas desde la app.
-- Incluye totales por estado y acumulado comprometido (excluye rechazados/anulados).

CREATE OR REPLACE VIEW v_resumen_aportes_afiliado AS
SELECT
    a.afiliado_id,
    af.nombre,
    af.estado                                                              AS estado_afiliado,
    TO_CHAR(a.fecha, 'YYYY')                                              AS anio,
    a.periodo,
    COUNT(*)                                                               AS total_aportes,
    COUNT(*) FILTER (WHERE a.estado = 'APROBADO')                         AS aportes_aprobados,
    COUNT(*) FILTER (WHERE a.estado = 'RECHAZADO')                        AS aportes_rechazados,
    COUNT(*) FILTER (WHERE a.estado = 'PENDIENTE')                        AS aportes_pendientes,
    COUNT(*) FILTER (WHERE a.estado = 'EN_REVISION')                      AS aportes_en_revision,
    COUNT(*) FILTER (WHERE a.estado = 'ANULADO')                          AS aportes_anulados,
    COALESCE(SUM(a.monto) FILTER (WHERE a.estado = 'APROBADO'), 0)        AS monto_aprobado,
    COALESCE(SUM(a.monto) FILTER (WHERE a.estado NOT IN ('RECHAZADO', 'ANULADO')), 0) AS monto_comprometido
FROM aporte a
JOIN afiliado af ON af.afiliado_id = a.afiliado_id
GROUP BY a.afiliado_id, af.nombre, af.estado, TO_CHAR(a.fecha, 'YYYY'), a.periodo
ORDER BY a.afiliado_id, a.periodo;

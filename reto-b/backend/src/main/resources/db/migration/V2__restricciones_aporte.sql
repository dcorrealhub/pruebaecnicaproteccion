-- V2: restricciones de integridad sobre aportes y saldos
-- Refuerzan en la base de datos las reglas que ya valida la aplicación.

ALTER TABLE aporte
    ADD CONSTRAINT chk_aporte_canal CHECK (canal IN ('APP_MOVIL', 'WEB', 'SUCURSAL'));

ALTER TABLE aporte
    ADD CONSTRAINT chk_aporte_monto_positivo CHECK (monto > 0);

ALTER TABLE saldo_mensual
    ADD CONSTRAINT chk_saldo_total_no_negativo CHECK (total >= 0);

-- V2: defensa en profundidad. Las reglas ya se validan en el dominio,
-- pero la base de datos no debe aceptar estados imposibles aunque llegue
-- una escritura por otro camino (script, migración, bug).

ALTER TABLE aporte
    ADD CONSTRAINT ck_aporte_monto_positivo CHECK (monto > 0);

ALTER TABLE saldo_mensual
    ADD CONSTRAINT ck_saldo_total_no_negativo CHECK (total >= 0);

ALTER TABLE aporte
    ADD CONSTRAINT ck_aporte_canal CHECK (canal IN ('APP_MOVIL', 'WEB', 'SUCURSAL'));

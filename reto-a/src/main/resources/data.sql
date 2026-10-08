-- Datos iniciales para pruebas
-- Saldos mensuales de afiliados sintéticos
-- H-017: datos semilla en src/main; se cargan en cualquier entorno al arrancar.
-- H-006: solo existe el saldo de 2025-06 y nada crea el del mes en curso; los aportes de hoy se acumulan aquí.
INSERT INTO saldo (afiliado_id, total_mes, mes) VALUES ('AF-001', 0.0, '2025-06');
INSERT INTO saldo (afiliado_id, total_mes, mes) VALUES ('AF-002', 0.0, '2025-06');
INSERT INTO saldo (afiliado_id, total_mes, mes) VALUES ('AF-003', 4500000.0, '2025-06');

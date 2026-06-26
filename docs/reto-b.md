

La funcionalidad: registro y consulta de aportes voluntarios
• Registrar un aporte de un afiliado (identificado por un id sintético) a un fondo voluntario:
monto, fecha y canal de origen. La operación debe ser idempotente.
• Reglas de negocio: el monto debe ser positivo; existe un tope mensual por afiliado
(parámetro configurable); un aporte que supere un umbral definido debe quedar marcado para
revisión posterior. Aportes que violen las reglas se rechazan con un mensaje claro.
• Consultar el consolidado de aportes de un afiliado en un periodo (total y detalle).
• Vista React: un formulario para registrar un aporte y una tabla con el consolidado. No
necesita ser bonito; necesita ser correcto y razonable.
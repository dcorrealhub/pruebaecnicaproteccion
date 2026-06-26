# Defensa de Decisiones — Resumen
**Candidato:** Juan Esteban Valdes  
**Fecha:** 2026-06-26

---

## Reto A — Auditoría de código

**1. ¿Cómo descompusiste el problema?**  
Rol claro, PDF como contexto, revisión archivo por archivo junto al modelo. Yo lideré — la IA complementó. No acepté hallazgos sin cuestionarlos.

**2. ¿En qué momentos le llevaste la contraria?**  
En el tipo monetario: el modelo propuso `BigDecimal`, yo apliqué el contexto del Banco de la República (sin centavos en Colombia) y opté por `long`. Además frené el ritmo — no quería un listado de hallazgos sin criterio, quería entender cada uno.

**3. ¿Qué tradeoffs tomaste?**  
No propuse migración completa a arquitectura hexagonal. Sin conocer el roadmap ni el nivel del equipo, imponer hexagonal es más riesgo que beneficio. Señalé los problemas de acoplamiento como hallazgos, propuse la dirección, pero no sobrediseñé.

**4. ¿Qué falta para producción en SFC?**  
SonarQube en el pipeline, cobertura de pruebas definida, IaC, observabilidad, gestión de secretos y documentación de API. Antes de todo eso: reunión con negocio y revisión con el arquitecto.

---

## Reto B — Construcción asistida

**1. ¿Cómo descompusiste el problema?**  
Compacté la sesión del Reto A antes de empezar — contexto limpio, intención clara. Usé **Opus para planear** (razonamiento sobre arquitectura y tradeoffs) y **Sonnet para implementar** (más rápido y económico para ejecución). Fui decisión por decisión antes de codificar: tipo monetario, semántica del tope, idempotencia, concurrencia, auth.

**2. ¿En qué momentos le llevaste la contraria?**  
- Al subir los cambios, el modelo quiso hacer merge directo a `master`. Lo frené — la entrega va en rama `candidato/nombre-apellido`, no en `master`. El modelo no tenía ese contexto; yo sí.
- `nuevoTotal > tope` (no `>=`): "superar" no incluye la igualdad exacta.  
- `idempotenciaKey` en mount, no en click — doble click generaría doble aporte.  
- `step="100000"` en monto — los aportes son de millones, no centavos.  
- `BigDecimal` aquí sí aplica, a diferencia del Reto A — aportes voluntarios pueden tener decimales.

**3. ¿Qué tradeoffs tomaste?**  
- TypeScript + Tailwind: el scaffold venía en JavaScript. Como la implementación era desde cero, me di el lujo de migrar — TypeScript previene errores con tipado estático, Tailwind agiliza los estilos. En un PR sobre código existente no habría tocado el stack.
- `APORTE_REVERSADO` fuera de alcance — no pedido, documentado como deuda consciente.  
- JWT en memoria del cliente (no localStorage) — más seguro ante XSS, el usuario re-autentica al recargar.  
- JWT fue el mínimo viable: en Protección no se puede entregar un frontend y un backend sin auth — exponer endpoints de aportes sin autenticación en una entidad financiera regulada no es una opción.  
- Credenciales solo por variable de entorno — el sistema no arranca sin auth configurada.  
- Paquete `auth` aislado (`infrastructure.web.auth`) — si cambia el mecanismo de autenticación, se toca un solo paquete sin afectar controladores de negocio.

**4. ¿Qué falta para producción en SFC?**  
Análisis de código estático (SonarQube), usuarios en BD (no en memoria), gestión de secretos real, HTTPS obligatorio, refresh tokens, rate limiting en `/auth/login` y logs de auditoría inmutables.

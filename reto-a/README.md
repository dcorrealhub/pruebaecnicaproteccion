# Reto A — Módulo de Aportes Voluntarios

**Proyecto:** Registro de Aportes Voluntarios al Fondo de Pensiones
**Stack:** Spring Boot 3.4.1 · Java 21 · JPA/Hibernate · H2 (in-memory)
**Rama:** `candidato/tomas-rios`
**Auditor / Tech Lead:** Tomás Ríos — CIS Protección S.A.

---

## Bitácora de Refactorización

Estado del proceso de corrección de los hallazgos **Críticos** y **Altos** identificados en [`AUDITORIA.md`](./AUDITORIA.md).

### Fase 0 — Documentación
- [x] **[docs]** Crear `AUDITORIA.md` con los 15 hallazgos detallados del módulo
- [x] **[docs]** Crear `README.md` con esta bitácora y las evidencias de PoC

### Fase 1 — Precisión Numérica *(Hallazgo N° 2 — Crítico)*
- [x] **[feat]** Reemplazar `double` por `BigDecimal` con escala `(19,2)` y redondeo `HALF_UP` en `Aporte.java`
- [x] **[feat]** Reemplazar `double` por `BigDecimal` con escala `(19,2)` en `Saldo.java`
- [x] **[feat]** Reemplazar `double` por `BigDecimal` con escala `(19,2)` en `EventoAporte.java`
- [x] **[feat]** Reemplazar `double` por `BigDecimal` en `AporteRequest.java`
- [x] **[feat]** Migrar `AporteService.java`: lógica de negocio con `BigDecimal.add()` y `compareTo()`
- [x] **[fix]** Corregir bug `==` en validación de tope mensual → `compareTo() > 0` *(Hallazgo N° 4 — Crítico)*
- [x] **[feat]** Agregar `spring-boot-starter-validation` a `pom.xml`
- [x] **[test]** Actualizar `AporteServiceTest.java` para usar `BigDecimal` en constructores y assertions

> **Commit:** `feat(reto-a): reemplazar double por bigdecimal con escala 19,2 en entidades dto y servicio`

### Fase 2 — Seguridad y Capa de Transporte *(Hallazgos N° 1, 7, 8 parcial, 11)*
- [x] **[fix]** Eliminar `JdbcTemplate` y `RowMapper` del `AporteController.java` *(Hallazgo N° 7 — Alto)*
- [x] **[fix]** Eliminar SQL concatenado vulnerable → delegar a `AporteService.consolidado()` *(Hallazgo N° 1 — Crítico)*
- [x] **[fix]** Agregar `@Valid` en `AporteController.registrar()` para activar validación declarativa
- [x] **[feat]** Implementar Bean Validation JSR-380 en `AporteRequest.java` *(Hallazgo N° 11 — Medio)*
- [x] **[refactor]** Agregar método `consolidado()` al servicio (separación parcial CQRS) *(Hallazgo N° 8 — Alto)*
- [x] **[chore]** Eliminar dependencia `spring-boot-starter-jdbc` de `pom.xml`
- [x] **[fix]** Corrección semántica HTTP: `POST /api/aportes` retorna `201 Created`

> **Commit:** `fix(reto-a): eliminar jdbctemplate del controller y agregar validacion jsr-380 en aporterequest`

### Fase 3 — Concurrencia e Idempotencia *(Hallazgos N° 3, 5, 6, 9, 10)*
- [x] **[fix]** Locking pesimista `@Lock(PESSIMISTIC_WRITE)` en `SaldoJpaRepository.findByAfiliadoIdForUpdate()` para serializar acceso concurrente al saldo *(Hallazgo N° 3 — Crítico)*
- [x] **[feat]** Implementar llave de idempotencia `idempotencyKey` (UUID v4) en `AporteRequest` y tabla `aporte` con constraint `UNIQUE` *(Hallazgo N° 5 — Crítico)*
- [x] **[fix]** Agregar `@Transactional(rollbackFor = Exception.class)` en `AporteService.registrar()` — requerido para que el lock pesimista proteja toda la operación *(Hallazgo N° 6 — Alto)*
- [x] **[fix]** Corregir orden de persistencia: `aporteRepo.save()` antes de `eventoRepo.save(new EventoAporte(saved))` *(Hallazgo N° 14 — Medio)*
- [x] **[fix]** Eliminar `afiliadoId` del mensaje de excepción expuesto al cliente *(Hallazgo N° 9 — Alto)*
- [x] **[fix]** Mover monto a nivel `DEBUG` en logs, INFO solo publica `aporteId`, `periodo` y `marcadaRevision` *(Hallazgo N° 10 — Alto)*

> **Commit:** `fix(reto-a): implementar transaccionalidad y locking pesimista contra condiciones de carrera`
> **Commit:** `feat(reto-a): agregar llave de idempotencia en registro de aportes`

### Fase 4 — Arquitectura y Calidad *(Hallazgos N° 12, 13, 14, 15)*
- [ ] **[refactor]** Crear jerarquía de excepciones de dominio + `@RestControllerAdvice` *(Hallazgo N° 12 — Medio)*
- [ ] **[fix]** Mover consola H2 a perfil `dev` únicamente *(Hallazgo N° 13 — Medio)*
- [ ] **[test]** Agregar `@Transactional` a `AporteServiceTest` para aislar estado entre tests *(Hallazgo N° 15 — Bajo)*

---

## Evidencias de Concepto (PoC) y Replicación de Fallos

> **Pre-requisito:** Arrancar la aplicación en su estado **original** (antes de la refactorización) con `mvn spring-boot:run` desde la carpeta `reto-a/`.

---

### PoC 1 — Inyección SQL en `GET /api/aportes/consolidado`

**Hallazgo:** N° 1 (Crítico) — `AporteController.java`, líneas 40–43

**Descripción del fallo:** El parámetro `afiliadoId` se concatena directamente en el SQL. Un payload con comillas simples rompe la query y permite acceder a datos de otros afiliados.

**Replicación — exfiltración de todos los aportes:**

```bash
# Payload: ' OR '1'='1
# URL-encoded: %27+OR+%271%27%3D%271

curl -s "http://localhost:8080/api/aportes/consolidado?afiliadoId=%27+OR+%271%27%3D%271&periodo=2025-06" | jq .
```

**Resultado esperado (vulnerable):** Retorna aportes de **todos** los afiliados, no solo del solicitado.

**Resultado esperado (corregido):** HTTP 200 con lista vacía `[]` o HTTP 400 — nunca filtra por la condición inyectada.

**Replicación — destrucción de tabla (si el driver lo permite):**

```bash
# Payload: AF-001'; DROP TABLE aporte; --
curl -s "http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001%27%3B+DROP+TABLE+aporte%3B+--&periodo=2025-06"
```

---

### PoC 2 — Bug lógico `==` en validación de tope mensual

**Hallazgo:** N° 4 (Crítico) — `AporteService.java`, línea 45

**Descripción del fallo:** El código usa `if (nuevo == topeMensual)` en lugar de `if (nuevo > topeMensual)`. Solo rechaza el aporte si el total es **exactamente** 10.000.000, pero deja pasar cualquier valor superior.

**Replicación — aporte que supera el tope sin rechazo:**

```bash
# AF-001 parte de saldo 0. Intentamos registrar 12.000.000 (supera el tope de 10.000.000)
curl -s -X POST http://localhost:8080/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId": "AF-001", "monto": 12000000.0, "canal": "WEB"}' | jq .
```

**Resultado esperado (vulnerable):** HTTP 200, el aporte se registra con `monto = 12000000`. El tope nunca se activó.

**Resultado esperado (corregido):** HTTP 422 / 400 con mensaje de tope excedido.

---

### PoC 3 — Condición de carrera en actualización de saldo (Race Condition)

**Hallazgo:** N° 3 (Crítico) — `AporteService.java`, líneas 40–50

**Descripción del fallo:** El flujo es `READ → MODIFY en memoria → WRITE` sin locking. Múltiples requests concurrentes para el mismo afiliado leen el mismo saldo base y cada una lo incrementa independientemente, ignorando los cambios de las demás.

**Efecto esperado:** Con saldo inicial 0 y tope de 10.000.000, si enviamos 5 requests concurrentes de 3.000.000 cada una (total = 15.000.000), el tope debería rechazar a partir de la cuarta. En el sistema vulnerable, todas pasan porque cada una lee `saldo = 0` antes de que cualquier otra haga commit.

**Replicación con Apache Benchmark (`ab`):**

```bash
# 1. Primero crear el body de la request en un archivo temporal
cat > /tmp/aporte_payload.json << 'EOF'
{"afiliadoId": "AF-001", "monto": 3000000.0, "canal": "APP_MOVIL"}
EOF

# 2. Enviar 5 requests concurrentes (concurrency = 5, total = 5)
ab -n 5 -c 5 \
   -p /tmp/aporte_payload.json \
   -T "application/json" \
   http://localhost:8080/api/aportes
```

**Verificar el saldo resultante:**

```bash
# Consultar la consola H2 o hacer una query directa
curl -s "http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06" | jq 'length'
# Vulnerable: retorna 5 (todos los aportes pasaron)
# Corregido: retorna <= 3 (los que superan el tope son rechazados)
```

**Replicación alternativa con `wrk` (más preciso para concurrencia real):**

```bash
# wrk: 5 threads, 5 conexiones, durante 2 segundos
wrk -t5 -c5 -d2s \
    -s <(echo '
      wrk.method = "POST"
      wrk.headers["Content-Type"] = "application/json"
      wrk.body = "{\"afiliadoId\": \"AF-001\", \"monto\": 3000000.0, \"canal\": \"APP_MOVIL\"}"
    ') \
    http://localhost:8080/api/aportes
```

---

### PoC 4 — Pérdida de precisión numérica con `double`

**Hallazgo:** N° 2 (Crítico) — múltiples archivos

**Descripción del fallo:** Operaciones aritméticas con `double` acumulan error de punto flotante invisible.

**Replicación — verificación en consola Java (jshell):**

```bash
jshell
```

```java
// Dentro de jshell:
double acumulado = 0.0;
for (int i = 0; i < 3; i++) {
    acumulado += 1_666_666.67;  // 3 aportes que deberían sumar exactamente 5_000_000.01
}
System.out.println("Con double: " + acumulado);
// Salida: 5000000.210000001 (ERROR — no es 5000000.01)

import java.math.BigDecimal;
BigDecimal acumuladoBD = BigDecimal.ZERO;
for (int i = 0; i < 3; i++) {
    acumuladoBD = acumuladoBD.add(new BigDecimal("1666666.67"));
}
System.out.println("Con BigDecimal: " + acumuladoBD);
// Salida: 5000000.01 (CORRECTO)
```

---

## Cómo ejecutar el módulo

```bash
# Desde la raíz del reto
cd reto-a

# Compilar y ejecutar tests
mvn clean test

# Arrancar la aplicación
mvn spring-boot:run

# La API queda disponible en:
# http://localhost:8080/api/aportes
# Consola H2 (solo perfil dev): http://localhost:8080/h2-console
```

## Endpoints disponibles

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/aportes` | Registra un aporte voluntario |
| `GET` | `/api/aportes/consolidado?afiliadoId=&periodo=` | Consulta aportes por afiliado y periodo |

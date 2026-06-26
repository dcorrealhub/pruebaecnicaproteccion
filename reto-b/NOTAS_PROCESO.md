# Notas de proceso — Reto B

Registro de prompts, decisiones y razonamiento crítico sobre la implementación.  
La IA fue usada como acelerador; las decisiones de qué pedir, qué validar y qué corregir son propias.

---

## Contexto de partida

El proyecto base entregaba:
- `ConsolidadoAportes.jsx` con un `TODO` explícito y campos de período como `<input type="text" pattern="\d{4}-\d{2}">`.
- `RegistrarAporte.jsx` funcional pero sin estilos.
- `App.jsx` con navegación mínima mediante botones sin estilo.
- Sin archivo CSS global.
- Backend con use cases implementados pero sin normalización de `afiliadoId`.

---

## 🎨 Frontend y experiencia de usuario

### 1. Mejorar el formulario de consulta

**Objetivo:** Hacer más intuitiva la selección del período para consultar el consolidado de aportes.

**Prompt:**
```
¿Me recomiendas organizar mejor el formulario para el consolidado de aportes?
Tal vez utilizando un selector de fechas para los campos del período.
```

**Qué se implementó:**
- `<input type="month">` en los campos `periodoDesde` y `periodoHasta`.
- Atributo `max` dinámico en "desde" (ajustado al valor de "hasta" o al mes actual).
- Atributo `min` dinámico en "hasta" (ajustado al valor de "desde").
- Validación cruzada explícita en `handleBuscar` antes de llamar la API.

**Decisión y razonamiento:**  
`type="month"` produce exactamente `YYYY-MM` sin transformación, lo que significa cero riesgo de desincronización entre el formato del input y lo que espera el backend. No se necesita ninguna librería de datepicker. La validación cruzada en el handler cubre navegadores que ignoran `min`/`max` (Safari legacy).

**Qué verifiqué del output de la IA:**  
- Que el valor de `mesActual` se calcule en tiempo de módulo (`new Date().toISOString().slice(0,7)`), no dentro del render, para evitar recalcularlo en cada re-render.
- Que la validación `periodoDesde > periodoHasta` funcione correctamente con strings `YYYY-MM` (el orden lexicográfico coincide con el cronológico para este formato).

---

### 2. Agregar estilos CSS base

**Objetivo:** Dar una apariencia más agradable sin perder la simplicidad.

**Prompt:**
```
¿Podrías mejorar el diseño agregando algunos colores o estilos CSS
para que la interfaz se vea agradable sin perder la simplicidad?
```

**Qué se implementó:**
- Creación de `src/index.css` con variables CSS, estilos de layout, formularios, tabla y alertas.
- Importación en `main.jsx`.
- Migración de inline styles a clases CSS en `App.jsx`, `RegistrarAporte.jsx` y `ConsolidadoAportes.jsx`.

**Decisión y razonamiento:**  
Preferí variables CSS sobre inline styles porque permiten cambiar la paleta desde un solo archivo (lo que resultó útil en el paso siguiente). No se introdujeron librerías de UI para mantener el bundle liviano y el código transparente.

---

### 3. Aplicar identidad visual corporativa

**Objetivo:** Adaptar la interfaz utilizando la identidad visual de Protección sin modificar la funcionalidad.

**Prompt:**
```
Implementa los colores corporativos de Protección en el frontend utilizando variables CSS.
Aquí tienes los códigos HEX exactos:

--color-primary-yellow: #DCE628;  (Amarillo/Verde lima del fondo)
--color-primary-blue:   #002F87;  (Azul oscuro del logo y fuentes)
--color-background-white: #FFFFFF;
--color-text-muted:     #7A7A7A;

Por favor, aplícalos en la estructura de componentes manteniendo un buen contraste visual.
```

**Qué se implementó:**
- Reemplazo de las variables genéricas por las cuatro variables corporativas más derivadas necesarias.
- Header con fondo `#002F87` y barra lateral `#DCE628` como acento.
- Tab activa: texto `#002F87` + subrayado `#DCE628`.
- Botón primario: fondo `#002F87`, texto blanco.
- Stat-chip (total aportado): fondo `#DCE628`, texto `#002F87`.
- Encabezado de tabla: fondo `#002F87`, texto blanco, borde inferior `#DCE628`.
- Badge "Revisión: Sí": fondo `#DCE628`, texto `#002F87`.

**Decisión crítica — contraste:**  
El amarillo `#DCE628` sobre blanco tiene ratio ~1.5:1, muy por debajo del mínimo WCAG AA (4.5:1). La IA lo usó inicialmente como color de texto en algunos contextos — lo corregí para que el amarillo funcione **únicamente como fondo** (con texto azul encima, ~7:1) o como acento decorativo. El texto nunca es amarillo sobre fondo claro.

| Combinación | Ratio | Cumple WCAG AA |
|---|---|---|
| Blanco sobre `#002F87` | ~12:1 | ✅ AAA |
| `#002F87` sobre `#DCE628` | ~7:1 | ✅ AA |
| `#7A7A7A` sobre blanco | ~4.5:1 | ✅ AA |
| `#DCE628` sobre blanco | ~1.5:1 | ❌ — no usado como texto |

---

## 🛡️ Validaciones y reglas de negocio

### 4. Detectar el problema de capitalización

**Objetivo:** Identificar un bug silencioso de consistencia de datos.

**Prompt:**
```
¿Qué pasa si un usuario ingresa su id en minúsculas al registrar
un aporte y al consultar el consolidado?
```

**Razonamiento:**  
Esta pregunta la formulé al revisar el código de `JpaAporteRepositoryAdapter` y ver que la búsqueda es una comparación de string directa en base de datos, sin ninguna normalización. El riesgo concreto: un usuario registra con `"af-001"` y luego consulta con `"AF-001"` (o vice versa) → obtiene lista vacía y total $0, sin ningún mensaje de error. El sistema no falla ruidosamente; falla silenciosamente, que es peor.

---

### 5. Implementar la normalización en frontend y backend

**Objetivo:** Garantizar que la regla se aplique en toda la pila, no solo en la UI.

**Prompt (frontend — surgió de la evaluación del riesgo):**  
Normalización con `.toUpperCase()` en el `onChange` de `afiliadoId` en ambos formularios, para dar feedback visual inmediato al usuario.

**Prompt (backend):**
```
Sí, aplícalo también en el backend y crea un test unitario para validar
que un afiliado registrado como "af-001" pueda consultarse correctamente
como "AF-001" y viceversa.
```

**Qué se implementó:**

*Frontend (`RegistrarAporte.jsx`, `ConsolidadoAportes.jsx`):*
```js
onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value.toUpperCase() }))}
```

*Backend — `RegistrarAporteUseCaseImpl`:*
```java
String afiliadoId = command.afiliadoId().toUpperCase();
// usado en findByAfiliadoIdAndMes, inicializar y constructor de Aporte
```

*Backend — `ConsultarAportesUseCaseImpl`:*
```java
String afiliadoId = query.afiliadoId().toUpperCase();
// usado en findByAfiliadoIdAndPeriodoBetween y ConsolidadoAportes
```

**Tests añadidos:**

- `RegistrarAporteUseCaseImplTest.registrar_afiliadoIdEnMinusculas_normalizaAMayusculas`  
  Envía `"af-001"`, verifica que el `Aporte` persistido lleva `"AF-001"` y que el repositorio de saldo fue invocado con `"AF-001"` (Mockito falla si el stub no coincide).

- `ConsultarAportesUseCaseImplTest.consultar_afiliadoIdEnMinusculas_normalizaAMayusculas`  
  Envía `"af-001"`, el stub solo responde para `"AF-001"`, verifica que el consolidado retornado lleva `"AF-001"` y el total correcto.

**Decisión de arquitectura:**  
La normalización va en el use case, no en el adapter ni en el controller, porque es una regla de negocio (consistencia del identificador del afiliado), no un detalle de infraestructura ni de presentación. Si el día de mañana hay otro canal que consume los use cases directamente, la regla se aplica igual.

**Defensa de la defensa en tres capas:**
- Frontend `onChange`: UX — el usuario ve el cambio al instante y no puede enviar datos inconsistentes por accidente.
- Use case de registro: garantía para cualquier cliente de la API (integraciones, scripts, otros frontends).
- Use case de consulta: la misma garantía en lectura — el `afiliadoId` en la respuesta también va normalizado.

---

## 💼 Dominio y reglas de negocio

### 6. Validación en el dominio

**Objetivo:** Garantizar que el invariante de negocio "un aporte siempre tiene monto positivo" se cumpla en el modelo de dominio, no solo en la capa de entrada.

**Prompt:**
```
Quiero implementar una validación dentro del constructor de la clase Aporte
para el atributo monto, para que siempre sea positivo este valor.
```

**Qué se implementó:**

*`domain/model/Aporte.java` — constructor:*
```java
if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("El monto del aporte debe ser positivo");
}
```

**Decisión de arquitectura:**  
La validación se puso en el constructor del modelo de dominio, no en el controller ni en el use case, porque es un invariante del objeto: un `Aporte` con monto ≤ 0 es un estado inválido por definición, y ninguna capa de la aplicación debería poder crearlo. Ponerlo en el constructor hace imposible que exista un `Aporte` inválido en memoria, independientemente de quién lo instancie.

Esto es diferente a una validación de entrada (`@NotNull`, `@Min` en el DTO), que solo protege el borde HTTP. Si hubiera otro caso de uso que creara aportes (importación por lote, migración), la regla se respetaría igual sin tener que recordar duplicarla.

**Qué verifiqué del output de la IA:**  
- Que la comparación use `compareTo(BigDecimal.ZERO) <= 0` y no `monto <= 0` (los `BigDecimal` no se comparan con `==` ni con operadores relacionales en Java).
- Que el test de dominio cubra los casos borde: `0`, negativo, y `null`.

---

## 📝 Documentación del código

### 7. Comentarios breves y descriptivos

**Objetivo:** Mejorar la legibilidad del código mediante comentarios que expliquen el *por qué*, no el *qué*.

**Prompt:**
```
Implementa comentarios cortos pero descriptivos de lo que hace cada cosa
implementada para el requerimiento y los tests.
```

**Qué se implementó:**  
Comentarios de una línea en los use cases, adapters y tests explicando decisiones no obvias:

```java
// Idempotencia: reintentos con la misma clave devuelven el aporte original sin duplicarlo.
// El periodo en formato YYYY-MM sirve como clave del saldo mensual y para consultas por rango.
// El @Version en SaldoMensualEntity lanza OptimisticLockException si dos transacciones concurrentes...
// BigDecimal.ZERO como identidad evita NPE cuando la lista está vacía.
// BETWEEN sobre YYYY-MM funciona correctamente en orden lexicográfico.
```

**Decisión y razonamiento:**  
La guía que seguí: un comentario solo justifica su existencia si explica algo que el código no puede expresar por sí solo — una restricción oculta, un invariante sutil, o una decisión que sorprendería a quien lea el código después. Los comentarios que solo parafrasean el nombre del método fueron descartados.

Ejemplos de lo que **no** se comentó (el nombre ya lo dice):
```java
// Guarda el aporte ← innecesario, guardar() es autoexplicativo
return aporteRepository.guardar(aporte);
```

Ejemplos de lo que **sí** se comentó (el *por qué* no es obvio):
```java
// El @Version en SaldoMensualEntity lanza OptimisticLockException si dos transacciones
// concurrentes intentan actualizar el mismo saldo; la segunda debe reintentar.
saldoRepository.guardar(saldo.conTotal(nuevoTotal));
```

**Qué verifiqué del output de la IA:**  
La IA tiende a comentar en exceso — genera comentarios tipo `// Registra el aporte` sobre un método llamado `registrarAporte`. Revisé cada comentario propuesto y eliminé los que solo repetían el nombre del símbolo. Los que sobrevivieron explican una decisión de diseño, un riesgo de concurrencia, o un comportamiento no evidente del código.

## 📸 Evidencia de ejecución y funcionamiento

A continuación, se presenta la evidencia visual del sistema operando de extremo a extremo, demostrando la integración entre el frontend y el backend con las reglas de negocio aplicadas.

### 1. Interfaz de Usuario (Frontend)

El frontend fue estilizado respetando la identidad visual corporativa y enfocado en una experiencia de usuario fluida y libre de errores.

#### Registro de Aportes
**Cómo funciona:** El usuario ingresa el ID del afiliado y el monto. Gracias a la normalización en tiempo real, el campo `afiliadoId` convierte automáticamente el texto a mayúsculas mientras el usuario escribe, previniendo inconsistencias en la base de datos.

![Captura: Formulario de registro de aporte con ID en mayúsculas y colores corporativos](screenshots\Screenshot_1.jpg)

#### Consolidado de Aportes
**Cómo funciona:** Se implementó el selector de meses (`type="month"`) para garantizar el formato `YYYY-MM`. El sistema valida dinámicamente que el "Período desde" no sea mayor al "Período hasta". Al consultar, se muestra la tabla con el historial y el "Total Aportado" resaltado en un *stat-chip*.

![Captura: Tabla de consolidado de aportes mostrando el total y los badges de revisión](screenshots\Screenshot_2.jpg)

---

### 2. Capa de Servicios y Lógica (Backend)

El backend actúa como el guardián de las reglas de negocio, validando los datos independientemente de cómo lleguen desde el cliente.

#### Ejecución del Servidor y Logs
**Cómo funciona:** Al levantar la aplicación Spring Boot, la base de datos (H2/Postgres) se inicializa correctamente. En los logs se puede observar cómo los endpoints responden a las peticiones del frontend, procesando la información a través de los Casos de Uso.

![Captura: Terminal o consola del IDE mostrando el log de arranque de Spring Boot y peticiones HTTP 200 OK](screenshots\Screenshot_3.jpg)

#### Pruebas Unitarias y de Dominio (Tests)
**Cómo funciona:** Ejecución exitosa de la suite de pruebas. Aquí se evidencia que la lógica del dominio (como rechazar aportes con monto negativo o nulo) y las reglas de negocio en los Use Cases (como la normalización del `afiliadoId`) están cubiertas y funcionando correctamente.

![Captura: Resultados de ejecución de JUnit/Mockito mostrando los tests en verde](screenshots\Screenshot_4.jpg)
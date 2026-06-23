# NOTAS_PROCESO

## Enfoque de trabajo

Antes de implementar cada componente se realizó un análisis del contrato, dependencias y responsabilidades de la clase dentro de la arquitectura Clean Architecture.

Se utilizó asistencia de IA como apoyo para:

* Comprender la arquitectura existente.
* Identificar dependencias entre capas.
* Validar el orden óptimo de implementación.
* Revisar posibles riesgos de diseño antes de escribir código.
* Analizar contratos y responsabilidades antes de implementar.

La implementación no se realizó directamente. Primero se analizó cada componente para comprender:

* Qué responsabilidad tenía.
* Qué puertos debía implementar.
* Qué entidades participaban.
* Qué reglas de negocio estaban involucradas.
* Qué impacto tendría sobre otros componentes.
* Qué dependencias eran necesarias para habilitar los casos de uso.

---

## Análisis inicial realizado

Se realizó una revisión completa de la estructura del proyecto para identificar los componentes pendientes de implementación.

Se identificó que los siguientes componentes contenían lógica incompleta:

* JpaSaldoRepositoryAdapter
* RegistrarAporteUseCaseImpl
* ConsultarAportesUseCaseImpl
* aportesApi.js
* vite.config.js

Posteriormente se definió el siguiente orden de implementación:

1. JpaSaldoRepositoryAdapter
2. RegistrarAporteUseCaseImpl
3. ConsultarAportesUseCaseImpl
4. vite.config.js
5. aportesApi.js

La decisión se tomó buscando habilitar primero los casos de uso críticos del backend y posteriormente la integración con el frontend.

---

## Análisis previo de JpaSaldoRepositoryAdapter

Antes de implementar el adaptador se analizó su responsabilidad dentro de la arquitectura.

Se identificó que el componente debía:

* Consultar saldos mensuales por afiliado y período.
* Persistir actualizaciones de saldo.
* Inicializar saldos cuando aún no existieran registros para un afiliado en un mes determinado.

También se analizaron las entidades involucradas:

* SaldoMensual (dominio)
* SaldoMensualEntity (persistencia)

y los mapeos requeridos entre ambas capas.

Adicionalmente se revisó el mecanismo de concurrencia optimista implementado mediante el campo `version` y la anotación `@Version` en JPA.

Se concluyó que el adaptador debía preservar el valor de `version` durante todos los procesos de mapeo y delegar el control de concurrencia a Hibernate.

---

## Análisis previo de RegistrarAporteUseCaseImpl

Antes de implementar el caso de uso principal se analizaron las reglas de negocio definidas en el enunciado.

Se identificaron como requisitos obligatorios:

* Implementar idempotencia mediante una clave única (`idempotenciaKey`).
* Validar que el monto del aporte fuera positivo.
* Validar que el acumulado mensual no superara el tope configurado.
* Marcar aportes para revisión cuando superaran el umbral definido.
* Actualizar correctamente el saldo mensual del afiliado.
* Mantener consistencia transaccional durante todo el proceso.

Se definió que la primera operación del flujo debía ser la validación de idempotencia para evitar registros duplicados ante reintentos del cliente.

También se decidió utilizar `BigDecimal` para todas las operaciones monetarias y mantener la actualización de saldo dentro de una transacción.

---

## Análisis previo de ConsultarAportesUseCaseImpl

Se revisó la responsabilidad de la consulta de consolidado.

Se identificó que el caso de uso debía:

* Consultar los aportes del afiliado dentro del rango solicitado.
* Calcular el total acumulado utilizando `BigDecimal`.
* Retornar tanto el total como el detalle de aportes.
* Ejecutarse como una transacción de solo lectura (`readOnly = true`).

Se concluyó que la lógica debía mantenerse simple y delegar la recuperación de información al repositorio correspondiente.

---

## Implementación realizada

### Backend

Se completaron los siguientes componentes:

#### JpaAporteRepositoryAdapter

* Implementación de persistencia de aportes.
* Mapeo entre modelo de dominio y entidad JPA.
* Búsqueda por clave de idempotencia.
* Consulta por afiliado y rango de períodos.

#### JpaSaldoRepositoryAdapter

* Consulta de saldos mensuales.
* Inicialización de saldos inexistentes.
* Persistencia de actualizaciones de saldo.
* Conservación del mecanismo de concurrencia optimista mediante `@Version`.

#### ConsultarAportesUseCaseImpl

* Consulta de aportes por afiliado y período.
* Cálculo del total utilizando `BigDecimal`.
* Construcción del consolidado con total y detalle.

#### RegistrarAporteUseCaseImpl

* Validación de monto positivo.
* Validación de tope mensual configurable.
* Implementación de idempotencia mediante `idempotenciaKey`.
* Marcado automático para revisión cuando el aporte supera el umbral configurado.
* Actualización del saldo mensual.
* Persistencia del aporte dentro de una transacción.

### Frontend

Se completó la integración con la API mediante:

* Implementación de `registrarAporte()` utilizando `fetch`.
* Implementación de `consultarConsolidado()` utilizando `fetch`.
* Manejo básico de errores HTTP.
* Uso del proxy configurado en Vite para comunicación con el backend.

---

## Prompts utilizados durante el análisis

### Análisis general del proyecto

"Analiza el proyecto completo reto-b siguiendo la arquitectura Clean Architecture. Identifica funcionalidades implementadas, clases pendientes, dependencias y orden recomendado de implementación."

### Validación de dependencias

"Analiza si JpaAporteRepositoryAdapter es realmente el primer componente que debería implementarse. Explica dependencias, impacto y riesgos antes de escribir código."

### Análisis específico de JpaSaldoRepositoryAdapter

"Analiza únicamente JpaSaldoRepositoryAdapter.java y todas las clases relacionadas. No escribas código todavía. Explica responsabilidades, entidades involucradas, mapeos requeridos, manejo de concurrencia optimista y riesgos de implementación."

### Análisis de RegistrarAporteUseCaseImpl

"Analiza RegistrarAporteUseCaseImpl.java. Explica el flujo de idempotencia, validación de reglas de negocio, actualización de saldo mensual y persistencia del aporte antes de generar código."

### Análisis de ConsultarAportesUseCaseImpl

"Analiza ConsultarAportesUseCaseImpl.java. Explica dependencias, cálculo del consolidado, uso de BigDecimal y consideraciones transaccionales antes de implementar."

---

## Conclusiones

La implementación se realizó manteniendo la estructura propuesta por la arquitectura Clean Architecture del proyecto.

Se priorizó:

* Separación entre dominio, aplicación e infraestructura.
* Uso de puertos y adaptadores.
* Operaciones monetarias mediante `BigDecimal`.
* Implementación de idempotencia para evitar duplicados.
* Soporte para concurrencia optimista mediante control de versión.
* Código simple, legible y alineado con los requisitos del ejercicio.

La asistencia de IA fue utilizada como apoyo para análisis, validación de diseño y aceleración de la implementación, manteniendo revisión y validación manual de las decisiones tomadas.

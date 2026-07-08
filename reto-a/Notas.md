# Notas del Reto A

## Objetivo

Realizar una auditoría técnica del módulo de registro de aportes voluntarios como si fuera una revisión de Merge Request.

---

## Estrategia de revisión

1. Comprender el flujo completo de registro de aportes.
2. Revisar las reglas de negocio implementadas.
3. Ejecutar y analizar las pruebas existentes.
4. Revisar la arquitectura del proyecto.
5. Identificar problemas relacionados con:
    - Seguridad
    - Correctitud funcional
    - Precisión numérica
    - Transaccionalidad
    - Concurrencia
    - Idempotencia
    - Clean Architecture
6. Priorizar únicamente los hallazgos que realmente bloquearían la aprobación del Merge Request.

---

## Proceso seguido

- Se revisó inicialmente la estructura del proyecto.
- Se identificaron los componentes principales:
    - Controller
    - Service
    - Repository
    - Domain
    - DTO
- Se revisó el flujo de registro de aportes.
- Se analizaron los tests existentes para identificar qué escenarios cubren y cuáles no.

---

## Uso de IA

La IA se utilizó como apoyo para:

- Proponer posibles puntos de revisión.
- Contrastar buenas prácticas para un sistema financiero.
- Ayudar a clasificar la severidad de los hallazgos.

Cada hallazgo fue verificado manualmente antes de documentarlo.

## Prompts utilizados

### Prompt 1
Analiza este proyecto Spring Boot como si fuera una revisión de Merge Request. Prioriza problemas críticos antes que mejoras en cuanto al estilo

### Prompt 2
Busca posibles problemas relacionados con manejo de dinero, concurrencia, transacciones e idempotencia

### Prompt 3
Revisa si existe alguna vulnerabilidad de seguridad o riesgo de SQL Injection en el proyecto

### Prompt 4
¿Las reglas de negocio del tope mensual están correctamente implementadas? 

### Prompt 5
Analiza muy bien la arquitectura del proyecto. Indica si quiza existen violaciones a Clean Architecture o principios SOLID

### Prompt 6
Revisa la cobertura de pruebas existente e indica qué escenarios importantes no están siendo validados

### Prompt 7
Ayúdame a clasificar los hallazgos por severidad (Crítica, Alta, Media y Baja) justificando cada decisión

---

## Criterios para clasificar severidad

### Crítica
Problemas que afectan directamente la integridad de los datos, la seguridad o las reglas de negocio y que impedirían aprobar el Merge Request.

### Alta
Problemas importantes que pueden generar fallos bajo determinadas condiciones, como concurrencia o reintentos.

### Media
Problemas de mantenibilidad, validaciones o arquitectura que deben corregirse antes de producción.

### Baja
Mejoras de diseño o buenas prácticas que no afectan el funcionamiento actual.

---

## Observaciones

Los tests existentes validan principalmente escenarios exitosos y algunos casos de error básicos. Se detectó ausencia de pruebas para reglas críticas como:

- Tope mensual.
- Idempotencia.
- Concurrencia.
- Transacciones.
- Casos límite relacionados con el dominio financiero.
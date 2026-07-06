# RETO A

## Entender el módulo
1. Revisión de Request, flujos, validaciones y restricciones.

## Extraer reglas de negocio
1. Registrar Aportes: no superar el tope mensual.
2. Consultar Consolidado: devolver el total aportado en un período.

## Compilar y ejecutar
1. Revisión de tests unitarios.
2. Consumo de endpoints con casos normales y de límite.

## Solicitar a la IA un barrido enfocado
Ejes: seguridad, corrección numérica, reglas de negocio, transaccionalidad, concurrencia, idempotencia.
1. Obtener listado de hallazgos relacionados.

## Corroborar que los hallazgos son reales y afectan al negocio

## Solicitar a la IA revisión arquitectónica, de configuración y cobertura de tests

## Categorizar y documentar
Categorizar en conjunto con la IA los hallazgos y documentarlos en el archivo `Hallazgos.md`.


## Reglas para categorizar:
1. Crítica (Bloquea MR): Daño grave en condiciones normales de consumo.
2. Alta (Bloqueda MR): Defectos funcionales en condiciones especificas (ej. Concurrencia).
3. Media (No Bloquea): Correcciones que se deben hacer.
4. Baja: Mejoras y buenas practicas.
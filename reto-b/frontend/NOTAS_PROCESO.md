# Uso de OpenCode para crear agentes

## Skills
Se hace uso de frontend-design, funciona muy bien para apps con react
npx skills add https://github.com/anthropics/skills --skill frontend-design

# Prompts:
## 1.
Actúa como un desarrollador frontend, experto en React. El proyecto que se te entrega consta de un formulario donde se registran aportes voluntarios y también una sección para visualizar los aportes realizados. Se tienen las siguientes reglas de negocio aplicadas en el backend: el monto debe ser positivo; existe un tope mensual por afiliado (parámetro configurable); un aporte que supere un umbral definido debe quedar marcado para revisión posterior. Aportes que violen las reglas se rechazan con un mensaje claro.. Dale un vistazo al proyecto para que adquieras contexto.

**Respuesta:** El agente revisa cada archivo del proyecto y da un status de lo que se debe implementar y de las cosas que ya están hechas. Procedo a implementar el consumo de API inicialmente. Acostumbro a realizar las tareas por pasos pequeños.

## 2.
Realiza la implementación de api solamente. Siempre ten en cuenta /frontend-design. Usa fetch y ten en cuenta que idempotenciaKey debe ser generado por el cliente. Lee los comentarios de los archivos

**Respuesta:** Crea la implementación con fetch del consumo de la api. La implementación es sencilla por lo que el código no necesita cambios

## 3. 
Ahora realiza las validaciones para el formulario de registro de aportes con las siguientes reglas en el archivo RegistrarAportes.jsx: Campos requeridos:
    - afiliadoId (texto, sintético — ej: "AF-001")
    - monto (número, positivo)
    - canal (selector: APP_MOVIL, WEB, SUCURSAL)
    - idempotenciaKey: generar automáticamente con crypto.randomUUID()
 
  Comportamiento esperado:
    - Validar monto > 0 antes de enviar
    - Mostrar mensaje de éxito o error según la respuesta
    - Si el aporte queda marcado para revisión, indicarlo claramente.

**Respuesta:** Al definir las reglas claramente como se esperan, el agente es capaz de implementarlas sin problema. por lo que procedo intentar mejorar el código usando buenas prácticas de desarrollo 

## 4. 

Ahora realiza la implementación sobre ConsolidadoAportes.jsx con los siguientes criterios: Campos de búsqueda:
    - afiliadoId (texto)
    - periodoDesde (formato YYYY-MM)
    - periodoHasta (formato YYYY-MM)
 
  Resultado esperado:
    - Total aportado en el periodo
    - Tabla con el detalle de cada aporte (fecha, monto, canal, marcadaRevision)
Si existe una implementación anterior, mejora el desarrollo

**Respuesta:** Validaciones más específicas y el código un poco más claro. Reitero que el uso de skills mejora enormemente el comportamiento de un agente
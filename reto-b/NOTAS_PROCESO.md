# Prompt a Claude para ejecución de prueba tecnica

1. Tengo unas pruebas tecnicas donde está permitido el uso de la IA. Este es el contexto; El CIS es el centro de ingeniería de Protección. Trabajamos bajo SAFe y una cultura de ingeniería
   AI-first: la inteligencia artificial es parte natural del flujo de trabajo, no una excepción. Nuestro stack
   núcleo es Spring Boot, React, PostgreSQL y Kubernetes sobre AWS, con GitLab CI/CD y prácticas
   DevSecOps. Operamos en un entorno financiero regulado por la Superintendencia Financiera de
   Colombia (SFC), donde la corrección, la seguridad y la trazabilidad no son negociables.
   Esta prueba no busca evaluar si sabes escribir un algoritmo de memoria. Busca evaluar cómo
   dirigís el trabajo, cómo juzgás el código propio, ajeno o generado y cómo defiendes tus decisiones.
   Es exactamente lo que hacés en el día a día de una célula. y esto es lo que debo generar: Vas a implementar una funcionalidad acotada sobre el stack del CIS (Spring Boot + PostgreSQL +
   un componente React). Podés y deberías apoyarte en la IA.
   La funcionalidad: registro y consulta de aportes voluntarios
   • Registrar un aporte de un afiliado (identificado por un id sintético) a un fondo voluntario:
   monto, fecha y canal de origen. La operación debe ser idempotente.
   • Reglas de negocio: el monto debe ser positivo; existe un tope mensual por afiliado
   (parámetro configurable); un aporte que supere un umbral definido debe quedar marcado para
   revisión posterior. Aportes que violen las reglas se rechazan con un mensaje claro.
   • Consultar el consolidado de aportes de un afiliado en un periodo (total y detalle).
   • Vista React: un formulario para registrar un aporte y una tabla con el consolidado. No
   necesita ser bonito; necesita ser correcto y razonable. Ya cloné el repositorio, ya levante la base de datos que está en Docker y vamos a realizarlo punto por punto.

2. No tomes desiciones arquitectonicas ni de diseño sin preguntarme primero

3. Que devuelva el aporte y mencione que ya se realizó a traves de un mensaje.
   Inicializar aporte en 0
   Que lea de nuevo y vuelva a intentar cuando surga algun error

4. Para solucionar el error de concurrencia separemos el metodo en 2 para que en caso de un error vuelva a leer la petición e intentarla nuevamente

5. Tenemos un ajuste :“Negocio nos avisa que los aportes por sucursal tienen mayor riesgo. A partir de ahora, un aporte por canal SUCURSAL se marca para revisión si supera 3.000.000. Los demás canales siguen con 5.000.000.” Para esto agreguemos una nueva propiedad configurable

6. Generación de Front
   Necesito una vista React simple que:

7. Explores la estructura del proyecto frontend existente para
   entender el stack (Vite/CRA/Next, JS/TS, si ya existe un cliente HTTP configurado
   con una URL base del backend). Si no existe ningún proyecto frontend todavía,
   créalo mínimo con Vite + React + TypeScript.

8. Tenga un formulario para registrar un aporte con los campos: afiliadoId (texto),
   monto (número), fecha (date), canal (select u texto libre). Al enviar, haga POST
   al backend. Si la respuesta es un error (400/409), muestre el mensaje de error
   del backend tal cual, sin inventarlo.

9. Tenga, debajo del formulario, un campo para afiliadoId + periodo (yyyy-MM) y un
   botón "Consultar" que haga GET al endpoint de consolidado y muestre:
   - El total del periodo.
   - Una tabla con el detalle de cada aporte (fecha, monto, canal, si quedó marcado
     para revisión).

10. Después de registrar un aporte exitosamente, refresque automáticamente la tabla
    de consolidado si el afiliadoId coincide con el consultado.

11. No te preocupes por el diseño visual — tiene que ser correcto y legible, no
    bonito. Usa estilos inline o CSS mínimo, sin librerías de UI nuevas.

12. Verifiques que corre (npm run dev / build) sin errores de compilación.

Restricciones:

- No inventes campos de negocio que no te haya dado (no agregues campos extra al
  formulario).
- Si los nombres exactos de las rutas o del shape del JSON del backend no coinciden
  con lo que asumiste, ajusta el cliente HTTP para que coincida — pregúntame si no
  estás seguro en vez de adivinar.

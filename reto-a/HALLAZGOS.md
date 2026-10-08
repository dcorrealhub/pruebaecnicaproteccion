# Reto A - Auditoria de Codigo

**Mariana Carvajal Rueda**

## Hallazgos Criticos

1. Dinero manejado con Double

Ubicación: Aporte.java ; EventoAporte.java; Saldo.java; AporteRequest.java
Severidad: Critica
¿Por qué es un problema?: El double tiene restricciones y puede presentar errores al momento de hacer operaciones como sumas y comparaciones ya que no es tan preciso con numeros decimales
¿Como corregirlo?: Usar DigDecimal o int que son mejores albergando datos como dinero que puede tener muchos puntos decimales y son mas precisos con las operaciones

2. Problema en Validación de Tope
   Ubicación: AporteService.java en Registrar cuando valida el tope
   Severidad: Critica
   ¿Por qué es un problema?: if (nuevo == topeMensual) {
   throw new IllegalArgumentException("El monto supera el tope mensual permitido");
   }

Solo esta validando es es igual, si nuevo es mayor que el tope mensual entonces basicamente pasaría
¿Como corregirlo?: Cambiar a algo asi como nuevo > topeMensual o usando los comparadores de BigDecimal

3. No evidencia Idempotencia
   Ubicación: AporteService.java en Registrar
   Severidad: Critica
   ¿Por qué es un problema?: En ningun momento se valida si este registro ya se habia producido antes, puede pasar que se envie dos veces la peticion de registar el aporte y puedan pasar estas dos peticiones porque no hay nada que lo impida.
   ¿Como corregirlo?: verificar contra lo ya persistido antes de procesar la solicitud.

4. Mismo metodo (registar) no maneja concurrencia
   Ubicación: AporteService.java en Registrar
   Severidad: Media
   ¿Por qué es un problema?: Al no tener el @Transactional que le dice a Spring que se maneje ese metodo como una sola unidad, lo que pasa es que pueden existir dos procesos al mismo tiempo lo que va a impedir validar si el monto va a superar el tope
   ¿Como corregirlo?: Agregar el @Transactional al metodo

5. Orden de guardado incosistente
   Ubicación: AporteService.java en Registrar
   Severidad: Baja
   ¿Por qué es un problema?: El codigo guarda primero el evento que dice que se registrado un reporte y despues guarda el reporte, si algo llega ha fallar puede que se guarde el evento y el reporte no
   ¿Como corregirlo?: Agregar el @Transactional al metodo

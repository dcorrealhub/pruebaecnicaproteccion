Prueba técnica Protección S.A.

Reto A

Hallazgo 1:
CRÍTICA - INYECCIÓN SQL
Ubicación: AporteController.java, método consolidado() (líneas 41-43)

String sql = "SELECT * FROM aporte WHERE afiliado_id = '" + afiliadoId + "' AND periodo = '" + periodo + "'";

Problema: Se están concatenando parámetros del request directo en SQL. Cualquiera puede inyectar 'OR'1'='1
y leer aportes de todos los afiliados, o algo peor. Lo irónico: AporteJpaRepository ya tiene findByAfiliadoIdAndPeriodo(...) parametrizado y sin usar, el desarrollador escribió SQL crudo en vez de usar lo que ya existía.

Solución: Cómo corregirlo: eliminar el JdbcTemplate del controller y usar el método del repositorio existente, o si se necesita JDBC, usar ? con jdbc.query(sql, ps -> {...}, afiliadoId, periodo).

--------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 2:

CRÍTICA - Dinero representado con double
Ubicación: Aporte.monto, Saldo.totalMes, AporteRequest.monto, EventoAporte.monto
Problema:  punto flotante binario no representa exactamente cantidades decimales. En acumulados mensuales de muchos aportes esto genera diferencias de centavos difíciles de auditar; inaceptable en un fondo regulado donde cada peso debe poder reconciliarse.

Solución: BigDecimal con escala fija (ej. 2 decimales) y RoundingMode explícito en todo el dominio, no solo en un punto.

--------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 3:
CRÍTICA - Comparación de igualdad para el tope mensual
Ubicación: AporteService.Java, línea 45.
if (nuevo == topeMensual) {
throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}

Problema:
Por qué es problema: dos bugs en uno. (1) Lógicamente está mal: debería bloquear cuando se supera el tope (nuevo > topeMensual), no solo cuando coincide exactamente. Con esto tal cual, un aporte que deja el saldo en 10.000.001 pasa sin problema y el tope queda burlado. (2) Comparar double con == es frágil por representación binaria de punto flotante; sumas decimales repetidas casi nunca dan exactamente el valor esperado, así que ni siquiera el caso "exacto" se detecta de forma confiable.

Solución: migrar a BigDecimal y comparar con compareTo(topeMensual) > 0



---------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 4:
CRÍTICA - Race condition / lost update sobre Saldo
Ubicación: AporteService.registrar() lee Saldo, suma en memoria, guarda; sin @Transactional, sin locking.
Problema: dos requests concurrentes para el mismo afiliado leen el mismo saldo antes de que ninguno haya persistido. El segundo save sobreescribe el incremento del primero → se pierde un aporte del cómputo del tope mensual, y en la práctica el tope se puede burlar simplemente mandando varias solicitudes en paralelo. Es el bug de concurrencia clásico en lógica financiera.

Solución: @Transactional en el método + locking optimista (@Version en Saldo) o pesimista (@Lock(PESSIMISTIC_WRITE)), con reintento ante conflicto.

----------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 5:
ALTA -  Sin idempotencia en POST /api/aportes

Ubicación: AporteController.registrar() / AporteService.registrar()

Problema: si el cliente reintenta (timeout, doble tap, retry automático del canal) se duplica el aporte completo: se suma dos veces al saldo y queda un registro fantasma. En un sistema que mueve rompe la capa de abstracción que pide el CIS: el controller no debería conocer detalles de persistencia. Además genera inconsistencia de estilo (comando vía service, consulta vía SQL crudo en el controller), lo que sugiere que la separación comando/consulta no se pensó de forma consciente.dinero real esto es un defecto de integridad serio, no un detalle.

Solución: aceptar una idempotency key (header o campo), persistirla junto al resultado, y si ya existe devolver la respuesta anterior sin re-aplicar el efecto.

----------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 6:
ALTA - Violación de Clean Architecture / separación comando-consulta

Ubicación: AporteController.java — inyecta JdbcTemplate directo y construye SQL en el controller para la consulta, mientras el comando sí pasa por la capa de servicio.

Problema: rompe la capa de abstracción que pide el CIS: el controller no debería conocer detalles de persistencia. Además genera inconsistencia de estilo (comando vía service, consulta vía SQL crudo en el controller), lo que sugiere que la separación comando/consulta no se pensó de forma consciente.

Solución: mover la consulta a un método de servicio (o un query-service explícito si quieren CQRS real) y eliminar el JdbcTemplate del controller.

----------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 7:
MEDIA - Falta de validación de entrada (Bean Validation)

Ubicación: AporteRequest.java sin anotaciones; la validación de monto está hardcodeada dentro del servicio.

Problema:  mezcla validación de forma (¿el campo llegó bien formado?) con reglas de negocio (¿supera el tope?) en el mismo lugar. No hay validación de afiliadoId ni canal (podrían llegar vacíos o con un canal no soportado).

Solución:  @NotBlank, @Positive, etc. en el DTO + @Valid en el controller; deja en el servicio solo las reglas de negocio.

----------------------------------------------------------------------------------------------------------------------------------------

Hallazgo 8:
MEDIA - Manejo de errores ausente / status codes incorrectos
Ubicación: no hay @ControllerAdvice/@ExceptionHandler en ningún lado.

Problema: las IllegalArgumentException (afiliado no encontrado, monto inválido, tope superado) se propagan sin capturar → Spring las convierte en 500 con stacktrace, exponiendo detalles internos y devolviendo un código HTTP semánticamente incorrecto (debería ser 400/404/409).

Solución: @ControllerAdvice que mapee cada excepción de negocio a su código HTTP correspondiente con mensaje controlado.


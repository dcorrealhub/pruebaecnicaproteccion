# Informe de Auditoría de Código - Reto A (CIS Protección)

---

### **Hallazgo 1: Vulnerabilidad de Inyección SQL**

* **Ubicación:** `controller/AporteController.java`
* **Severidad:** Alta
* **Evidencia del problema:**
  ```java
  String sql = "SELECT * FROM aporte WHERE afiliado_id = '" + afiliadoId + "' AND periodo = '" + periodo + "'";
  return jdbc.query(sql, aporteRowMapper);
  ```
* **Porque:** Se está construyendo una consulta SQL mediante la concatenación directa de Strings con parámetros que vienen de la petición. Se podrían generar inyecciones maliciosas donde el atacante pueda acceder a toda la información de todos los afiliados al fondo o alterar la base de datos.
* **Solución:** Usar consultas parametrizadas a través de un repositorio seguro (Spring Data JPA) o pasar los argumentos por separado si se usa JDBC.

---

### **Hallazgo 2: Violación del principio de Responsabilidad Única**

* **Ubicación:** `controller/AporteController.java`
* **Severidad:** Media
* **Evidencia del problema:**
  ```java
  private final JdbcTemplate jdbc;
  // ...
  return jdbc.query(sql, aporteRowMapper);
  ```
* **Porque:** Se está violando el principio de Single Responsibility y la separación de capas del CIS. El controlador tiene inyectado `JdbcTemplate` y conoce detalles de la base de datos; la lógica de acceso a datos debería estar encapsulada exclusivamente en la capa de repositorio.
* **Solución:** Eliminar la persistencia del controlador. Delegar la acción al Service para que este se comunique con el repositorio correspondiente.

---

### **Hallazgo 3: Uso de tipo flotante para dinero**

* **Ubicación:** `domain/Aporte.java`
* **Severidad:** Baja (Alta en contexto financiero)
* **Evidencia del problema:**
  ```java
  // Representa el monto del aporte en pesos colombianos
  private double monto;
  ```
* **Porque:** Se está utilizando el tipo `double` para almacenar el dinero de los aportes. Los tipos de punto flotante causan imprecisiones matemáticas en el redondeo binario, lo que generará micro-descuadres acumulativos en los saldos del fondo voluntario.
* **Solución:** Cambiar el tipo de dato del atributo a `BigDecimal` para asegurar precisión aritmética exacta.

---

### **Hallazgo 4: Error lógico en la validación del Tope Mensual**

* **Ubicación:** `Service/AporteService.java`
* **Severidad:** Alta
* **Evidencia del problema:**
  ```java
  if (nuevo == topeMensual) {
      throw new IllegalArgumentException("El monto supera el tope mensual permitido");
  }
  ```
* **Porque:** La validación del tope mensual utiliza una comparación exacta (`==`) con tipos de datos flotantes. Si el nuevo monto acumulado supera el tope por decimales microscópicos o cae exactamente por encima, la condición fallará y dejará pasar un valor ilegal que rompe la regla de negocio.
* **Solución:** Cambiar la condición utilizando un operador mayor o igual (`>=`) en lugar de una igualdad estricta, y realizar la comparación utilizando el método `.compareTo()` de `BigDecimal`.

---

### **Hallazgo 5: Falta de transaccionalidad en el registro**

* **Ubicación:** `Service/AporteService.java`
* **Severidad:** Alta
* **Evidencia del problema:**
  ```java
  // Cabecera del método sin anotación de control
  public Aporte registrar(AporteRequest req) {
      // ...
      saldoRepo.save(s);
      // ...
      eventoRepo.save(new EventoAporte(aporte));
      // ...
      return aporteRepo.save(aporte);
  }
  ```
* **Porque:** No existe la anotación `@Transactional` en el método `registrar`, el cual realiza tres escrituras independientes en la base de datos. Si el proceso falla a mitad de camino, la base de datos quedará inconsistente (ej. se actualiza el saldo pero no se crea el aporte).
* **Solución:** Añadir la anotación `@Transactional` de Spring a nivel de método o clase para asegurar el principio ACID y que todo se ejecute en una sola transacción.

---

### **Hallazgo 6: Riesgo de concurrencia en actualización de saldo**

* **Ubicación:** `Service/AporteService.java`
* **Severidad:** Alta
* **Evidencia del problema:**
  ```java
  Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId()).orElseThrow(...);
  double nuevo = s.getTotalMes() + monto;
  // ...
  saldoRepo.save(s);
  ```
* **Porque:** Existe un riesgo de condición de carrera en la actualización del saldo. Si entran dos aportes concurrentes del mismo afiliado al mismo tiempo, ambos leerán el mismo saldo inicial en memoria y el último en guardar pisará los datos del primero, ocasionando pérdida de dinero en el sistema.
* **Solución:** Implementar Bloqueo Pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) en el repositorio al consultar el saldo para congelar la fila temporalmente.

---

### **Hallazgo 7: Orden incorrecto en el guardado de entidades**

* **Ubicación:** `Service/AporteService.java`
* **Severidad:** Media
* **Evidencia del problema:**
  ```java
  eventoRepo.save(new EventoAporte(aporte)); // Aporte aún no tiene ID
  // ...
  return aporteRepo.save(aporte);
  ```
* **Porque:** Se está creando y guardando el objeto `EventoAporte` pasando la entidad `aporte` antes de que esta sea guardada e inicializada por la base de datos. Al ser el ID de Aporte autoincremental, el evento se registrará con una referencia nula, rompiendo la trazabilidad del proceso.
* **Solución:** Invertir el orden de guardado en el Service: persistir primero el objeto `aporte` en `aporteRepo`, capturar la entidad retornada con su ID ya generado, y pasar esta última al constructor de `EventoAporte`.

Se usó IA (Gemini) para mejorar la redacción del texto.
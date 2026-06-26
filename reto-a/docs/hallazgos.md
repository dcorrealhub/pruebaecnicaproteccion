# Hallazgos

## 1. Respuesta de los endpoints
### Ubicación: AportesController -> POST /api/aportes - GET /api/aportes/consolidado
### Severidad: Alta
Si bien ambos endpoints dan una respuesta correcta, como buena práctica se debe implementar el uso de ResponseEntity con la respuesta a cada petición.
**Ejm:** 200 OK, 404 Not Found, 500 Internal Server Error, 201 Created, etc.
Se debe ser más específico en la respuesta.

## 2. Lógica en el controlador
### Ubicación: AportesController -> GET /api/aportes/consolidado
### Severidad: Critica
Un error grave es tener lógica en el controlador que no se encuentra en el repositorio. Se debe respetar la arquitectura propuesta.
Para este caso se debe hacer uso de la clase AporteService con un método que llame al repositorio y en lo posible usar siempre JPA para las consultas.
Los parámetros que recibe la petición deben ser con tipos más específicos (**Ejm:** String afiliadoId, Date periodo) con el fin de limitar los datos que el usuario puede entregar.

## 3. Test pesado y sin aislamiento
### Ubicación: AporteServiceTest -> @SpringBootTest
### Severidad: Alta
El test usa `@SpringBootTest` que levanta toda la aplicación para probar solo un servicio. Además los tests comparten la misma base de datos sin limpiar datos entre uno y otro.
**Solución:** Usar mocks con Mockito para aislar el servicio y evitar depender de la BD. Agregar `@BeforeEach` que limpie o resetee los datos antes de cada test.

## 4. Tests dependen de datos precargados
### Ubicación: AporteServiceTest -> data.sql
### Severidad: Alta
Los tests asumen que existen los afiliados "AF-001" y "AF-002" en la BD. Si alguien modifica el archivo `data.sql` los tests se rompen. Ademas `data.sql` esta en la carpeta de main, no de test.
**Solución:** Mover los datos de prueba a `src/test/resources` o mejor aun, usar mocks para no depender de la BD en los tests de servicio.

## 5. Bug de tope mensual
### Ubicación: AporteService.java linea 45
### Severidad: Critica
El servicio compara `nuevo == topeMensual` con doubles, lo cual tiene problemas de precision. Ademas la condicion deberia ser `>` (mayor que) no `==` (igual a). Si un aporte supera el tope mensual, el sistema NO lanza la excepcion. No hay ningun test que cubra este caso.
**Solución:** Cambiar `==` por `>` en la condicion y agregar un test que pruebe un monto que exceda el tope mensual.

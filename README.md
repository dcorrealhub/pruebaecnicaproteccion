# Prueba Técnica AI-First · CIS Protección S.A.

Repositorio base para la prueba técnica de nivel senior del Centro de Ingeniería de Software (CIS).

## Entregables

La carpeta [`entregables/`](entregables/) contiene los documentos de la prueba:

| Archivo | Contenido |
|---------|-----------|
| [`AUDITORIA.md`](entregables/AUDITORIA.md) | Hallazgos del Reto A — revisión de código como MR |
| [`DEFENSA.md`](entregables/DEFENSA.md) | Reto C — defensa extendida de decisiones técnicas |
| [`DEFENSA_CORTA.md`](entregables/DEFENSA_CORTA.md) | Reto C — versión resumida para presentación |

## Estructura del repositorio

```
pruebaecnicaproteccion/
├── reto-a/          # Módulo a auditar — Spring Boot con defectos deliberados
└── reto-b/
    ├── backend/     # Scaffold Spring Boot (Clean Architecture) para implementar
    └── frontend/    # Scaffold React + Vite para implementar
```

## Requisitos previos

- Java 21
- Maven 3.9+
- Node 20+
- Docker y Docker Compose (para Reto B)

## Reto A — Auditoría de código

El módulo `reto-a` es un servicio de registro de aportes a un fondo voluntario.
**Compila y los tests felices pasan.** Tu trabajo es revisarlo como si fuera un MR de tu célula.

```bash
cd reto-a
./mvnw test        # Verifica que los tests pasan
./mvnw spring-boot:run  # Levanta el servicio (puerto 8080, H2 en memoria)
```

Endpoints disponibles para exploración manual:

```
POST http://localhost:8080/api/aportes
GET  http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06
```

Payload de ejemplo para el POST:
```json
{
  "afiliadoId": "AF-001",
  "monto": 500000.0,
  "canal": "APP_MOVIL"
}
```

## Reto B — Construcción asistida

### Qué se modificó y por qué

Todo el código implementado está en `reto-b/`. El scaffold base se entregó vacío y se implementó desde cero, lo que permitió tomar decisiones de stack adicionales que en un PR sobre código existente no habrían sido viables.

Decisiones tomadas sobre el scaffold base:

- **TypeScript en el frontend:** el scaffold venía en JavaScript. Migrar a TypeScript en una implementación desde cero tiene costo casi nulo y aporta tipado estático, detección temprana de errores y mejor experiencia de desarrollo. En un PR sobre código existente habría sido fuera de alcance.
- **Tailwind CSS:** agregado para estilos. Su modelo de clases utilitarias inline permite construir UI consistente rápidamente sin mantener archivos CSS separados.
- **Autenticación JWT:** el enunciado no pedía auth explícitamente, pero en el contexto de Protección no se puede entregar un frontend y un backend sin ningún mecanismo de autenticación. Un endpoint de aportes voluntarios sin auth en una entidad financiera regulada no es una opción, así sea una prueba técnica. El token expira en 9 horas — una jornada laboral.

### Configuración

El backend requiere las siguientes variables de entorno (ver `.env.example`):

```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
API_USERNAME=
API_PASSWORD=
JWT_SECRET=
```

### Base de datos (PostgreSQL vía Docker)

```bash
docker compose up -d
```

Esto levanta PostgreSQL en `localhost:5432` con base de datos `proteccion_reto`, usuario `postgres`, contraseña `postgres`.

### Backend

```bash
cd reto-b/backend
./mvnw spring-boot:run   # Puerto 8082
```

### Frontend

```bash
cd reto-b/frontend
npm install
npm run dev              # Puerto 5173
```

## Guía de instalación — Reto B desde cero

### Prerrequisitos

- Java 21
- Maven 3.9+
- Node 20+
- Docker y Docker Compose

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd pruebaecnicaproteccion
git checkout candidato/juan-esteban-valdes
```

### 2. Base de datos (PostgreSQL local vía Docker)

```bash
docker compose up -d
```

Levanta PostgreSQL en `localhost:5432`, base de datos `proteccion_reto`, usuario `postgres`, contraseña `postgres`. Flyway crea las tablas automáticamente al arrancar el backend.

### 3. Variables de entorno del backend

```bash
cp reto-b/backend/.env.example reto-b/backend/.env
```

Edita `reto-b/backend/.env` con los valores reales:

```
DB_URL=jdbc:postgresql://localhost:5432/proteccion_reto
DB_USERNAME=postgres
DB_PASSWORD=postgres
API_USERNAME=cis-user
API_PASSWORD=Cis-Pass#2026
JWT_SECRET=cis-proteccion-secret-key-2026-super-secure-32bytes
```

### 4. Levantar el backend

```bash
cd reto-b/backend
./mvnw spring-boot:run
```

El backend queda disponible en `http://localhost:8082`. Para verificar:

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cis-user","password":"Cis-Pass#2026"}'
# Respuesta esperada: {"token":"eyJ..."}
```

### 5. Levantar el frontend

```bash
cd reto-b/frontend
npm install
npm run dev
```

El frontend queda disponible en `http://localhost:5173`. El proxy de Vite redirige `/api` y `/auth` al backend automáticamente — no se necesita configuración adicional.

### 6. Usar la aplicación

1. Abrir `http://localhost:5173`
2. Iniciar sesión con `cis-user` / `Cis-Pass#2026`
3. Registrar aportes en la pestaña **Registrar**
4. Consultar el consolidado por afiliado y periodo en la pestaña **Consolidado**

### Despliegue en Railway + Vercel

El backend está desplegado en Railway y el frontend en Vercel. Las variables de entorno del backend en Railway son las mismas del `.env` pero apuntando a la BD de Railway. Consultar `.env.example` para la estructura.

---

## Instrucciones para candidatos

1. **No hagas fork.** Clona este repositorio directamente.
2. Crea una rama con tu nombre en formato `candidato/nombre-apellido`:
   ```bash
   git checkout -b candidato/maria-garcia
   ```
3. **Reto A:** Documenta tus hallazgos en `reto-a/HALLAZGOS.md`. No modifiques el código del módulo a auditar.
4. **Reto B:** Implementa la funcionalidad en `reto-b/`. Puedes y debes apoyarte en IA.
5. Conserva tus prompts y notas en `reto-b/NOTAS_PROCESO.md`. Son parte de la entrega.
6. Sube tu rama al repositorio remoto cuando termines:
   ```bash
   git push origin candidato/maria-garcia
   ```

## Stack de referencia CIS

- **Backend:** Spring Boot 3.4.x · Java 21 · Spring Data JPA · PostgreSQL
- **Frontend:** React 18 · Vite · fetch nativo
- **Arquitectura:** Clean Architecture · SOLID · CQRS
- **Seguridad:** OWASP Top 10 · manejo correcto de dinero (BigDecimal) · idempotencia

> Usa datos sintéticos en todo momento. Nunca uses ni inventes información personal real.

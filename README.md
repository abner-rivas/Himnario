# Himnario Digital API

Backend inicial de Himnario Digital, construido con Kotlin, Ktor y PostgreSQL. Esta iteración implementa el catálogo de coros y deja una base modular para autenticación, categorías, partituras y audio.

## Requisitos

- Java 21
- Docker
- Docker Compose

El repositorio incluye Gradle Wrapper; no se requiere una instalación global de Gradle.

## Inicio rápido

1. Copia las variables de desarrollo si deseas personalizarlas:

   ```bash
   cp .env.example .env
   ```

2. Construye y levanta la API junto con PostgreSQL:

   ```bash
   docker compose up -d --build
   ```

La API queda disponible en `http://localhost:8080` y ambos contenedores aparecen agrupados bajo el proyecto `himnario-api`.

Para ejecutar la API directamente durante desarrollo, levanta solamente PostgreSQL:

```bash
docker compose up -d postgres
```

Luego ejecuta la API.

Linux/macOS:

```bash
./gradlew run
```

Windows:

```powershell
gradlew.bat run
```

Flyway aplica las migraciones al iniciar y HikariCP mantiene el pool de conexiones.

## Comandos útiles

```bash
./gradlew test
./gradlew build
docker compose config
docker build -t himnario-api:local .
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/health` | Liveness de la API |
| `GET` | `/api/v1/health/ready` | Readiness con comprobación de PostgreSQL |
| `GET` | `/api/v1/hymns` | Lista paginada y filtrable (`page`, `size`, `q`, `status`, `tempo`, `key`) |
| `GET` | `/api/v1/hymns/{id}` | Detalle de un coro |
| `POST` | `/api/v1/hymns` | Creación provisional |
| `PUT` | `/api/v1/hymns/{id}` | Actualización provisional |
| `PATCH` | `/api/v1/hymns/{id}/archive` | Archivado lógico |
| `GET` | `/swagger` | Documentación Swagger UI |

`POST` y `PUT` son públicas únicamente durante esta fase. Se moverán a `/api/v1/admin/hymns` y se protegerán cuando se implemente autenticación.

Prueba de salud:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/health/ready
```

Creación de ejemplo:

```bash
curl -X POST http://localhost:8080/api/v1/hymns \
  -H 'Content-Type: application/json' \
  -d '{"title":"Santo Espíritu","musicalKey":"G","bpm":72,"tempo":"SLOW","status":"ACTIVE"}'
```

## Configuración

Todas las opciones se leen del entorno. Consulta [.env.example](.env.example). `.env` está ignorado por Git y no se cargan credenciales desde el código.

`CORS_ALLOWED_HOSTS` recibe hosts separados por coma en formato `host[:port]`, sin esquema. No se admiten comodines. Los valores predeterminados solo contemplan hosts locales comunes de Expo durante desarrollo.

## Arquitectura

El flujo de una solicitud de coros es:

```text
Route -> Controller -> Service -> Repository -> Exposed -> HikariCP -> PostgreSQL
```

- `Route`: declara rutas y delega.
- `Controller`: traduce HTTP, DTO y códigos de respuesta.
- `Service`: aplica validaciones, reglas de negocio, archivado y generación de slugs.
- `Repository`: concentra exclusivamente el acceso a datos.
- Flyway es la única fuente de verdad para cambios de esquema; Exposed no crea tablas.

```text
src/main/kotlin/com/himnario/
├── Application.kt
├── common/
│   ├── dto/
│   ├── exceptions/
│   ├── health/
│   └── response/
├── config/
│   ├── AppConfig.kt
│   └── DatabaseConfig.kt
├── features/
│   └── hymns/
│       ├── dto/
│       ├── model/
│       ├── HymnRoutes.kt
│       ├── HymnController.kt
│       ├── HymnService.kt
│       ├── HymnRepository.kt
│       ├── ExposedHymnRepository.kt
│       └── HymnsTable.kt
└── plugins/
```

## Base de datos

La migración `V1__create_hymns.sql` crea `hymns` con UUID, slug único, metadatos musicales, estado, versión y timestamps con zona horaria. No existe ningún `DELETE` en el flujo normal: `PATCH /archive` cambia el estado a `ARCHIVED`.

PostgreSQL 18 usa `/var/lib/postgresql` como destino del volumen en su imagen oficial; `docker-compose.yml` sigue ese diseño para conservar los datos.

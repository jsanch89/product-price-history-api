# Sistema de Productos con Precios Históricos

API REST en Spring Boot para gestionar productos y su historial de precios, con foco en **máximo rendimiento** bajo un límite de contenedor de **1 CPU / 1 GB**.

## Contenido

- [Ejecución](#ejecución)
- [Endpoints](#endpoints)
- [Decisiones técnicas y justificación](#decisiones-técnicas-y-justificación)
- [Modelo de datos](#modelo-de-datos)
- [Reglas de negocio y concurrencia](#reglas-de-negocio-y-concurrencia)
- [Manejo de errores](#manejo-de-errores)
- [Rendimiento](#rendimiento)
- [Testing](#testing)
- [Benchmark de carga (k6)](#benchmark-de-carga-k6)
- [Supuestos y desviaciones del enunciado](#supuestos-y-desviaciones-del-enunciado)
- [Mejoras posibles](#mejoras-posibles)
- [Estructura del proyecto](#estructura-del-proyecto)

## Ejecución

### Con Docker (recomendado)

```bash
docker compose up --build
```

Levanta la API en `http://localhost:8080` con el límite de recursos de la prueba (1 CPU / 1 GB) y, en un contenedor auxiliar, corre `benchmark.sh` contra los 4 endpoints en cuanto el healthcheck de `/actuator/health` está `UP`.

### Local, sin Docker

Requiere JDK 21.

```bash
./gradlew bootRun
```

La app arranca con H2 embebida en memoria (sin dependencias externas) en `http://localhost:8080`.

### Build y tests

```bash
./gradlew build      # compila, corre tests y empaqueta el jar ejecutable
./gradlew test        # solo tests
```

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/products` | Crea un producto (`name` obligatorio, `description` opcional) |
| `POST` | `/products/{id}/prices` | Agrega un precio (`value`, `initDate`, `endDate` opcional) |
| `GET`  | `/products/{id}/prices?date=YYYY-MM-DD` | Precio vigente en una fecha → `{ "value": ... }` |
| `GET`  | `/products/{id}/prices` | Historial completo de precios del producto |

Ejemplos rápidos (equivalentes a los que ejecuta `benchmark.sh`):

```bash
curl -X POST localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Zapatillas deportivas","description":"Modelo 2025 edición limitada"}'

curl -X POST localhost:8080/products/1/prices \
  -H 'Content-Type: application/json' \
  -d '{"value":99.99,"initDate":"2024-01-01","endDate":"2024-06-30"}'

curl "localhost:8080/products/1/prices?date=2024-04-15"

curl localhost:8080/products/1/prices
```

Documentación interactiva (OpenAPI/Swagger, `springdoc-openapi`) disponible con la app en ejecución:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

## Decisiones técnicas y justificación

| Decisión | Elección | Por qué |
|----------|----------|---------|
| Framework | Spring Boot 3 (Web MVC + virtual threads) | Ecosistema maduro de testing y validación; con `spring.threads.virtual.enabled=true` escala bien en I/O sin necesidad de dimensionar pools, incluso con 1 CPU. |
| Acceso a datos | Spring Data JPA / Hibernate | Modelado explícito de entidades y relaciones (criterio de evaluación); las lecturas calientes usan *projections* directas en vez de cargar entidades completas. |
| Base de datos | H2 en memoria, modo PostgreSQL (`MODE=PostgreSQL`) | Arranque instantáneo, cero contenedores extra, sin latencia de red a una BD externa. La persistencia durable no es requisito de la prueba. Migrar a Postgres real solo requiere cambiar el datasource; el SQL ya está escrito en dialecto compatible. |
| Migraciones | Flyway (`V1__init_schema.sql`) | Esquema e índices versionados explícitamente en vez de `ddl-auto: update`. |
| Build | Gradle | Ya presente en el proyecto base. |

## Modelo de datos

```
product
  id          BIGINT IDENTITY PK
  name        VARCHAR(255) NOT NULL
  description VARCHAR(1000)

price
  id          BIGINT IDENTITY PK
  product_id  BIGINT FK -> product(id) NOT NULL
  value       DECIMAL(12,2) NOT NULL CHECK (value >= 0)
  init_date   DATE NOT NULL
  end_date    DATE NULL              -- null = vigente sin límite
  CHECK (end_date IS NULL OR end_date >= init_date)

ÍNDICE (product_id, init_date, end_date) -- soporta lookup de vigencia y orden del historial
```

Ver `src/main/resources/db/migration/V1__init_schema.sql`.

## Reglas de negocio y concurrencia

- Dos rangos de precio `[a.init, a.end]` y `[b.init, b.end]` (con `end = null` ≡ infinito) se consideran solapados si `a.init <= COALESCE(b.end, MAX) AND b.init <= COALESCE(a.end, MAX)`. La verificación se hace con una única query `EXISTS` sobre el índice compuesto, dentro de la misma transacción del insert.
- `initDate < endDate` se valida antes de tocar la base (cuando `endDate` no es null) → `400`.
- **Concurrencia**: al insertar un precio se toma un lock pesimista sobre el producto (`SELECT ... FOR UPDATE` vía `@Lock(PESSIMISTIC_WRITE)`) antes de chequear solapamiento, de modo que dos inserts concurrentes sobre el mismo producto no puedan crear rangos solapados entre sí. Verificado con un test de integración que lanza 8 inserts concurrentes solapados: exactamente uno recibe `201` y el resto `409` (`PriceConcurrencyIntegrationTest`).
- Producto inexistente → `404`. Solapamiento de fechas → `409 Conflict`. Sin precio vigente en la fecha consultada → `404`.

Alternativa no implementada: un *exclusion constraint* nativo de PostgreSQL (`EXCLUDE USING gist`) evitaría el lock explícito, pero es específico de ese motor y aquí se usa H2.

## Manejo de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`, extiende `ResponseEntityExceptionHandler`) centraliza las respuestas de error en formato **Problem Details** (RFC 7807, `ProblemDetail`):

- `400` — errores de validación de request (Bean Validation) y `initDate >= endDate`.
- `404` — producto o precio vigente no encontrado.
- `409` — solapamiento de precios.

## Rendimiento

Estrategia:

1. **Arranque**: `spring.main.lazy-initialization: true`, banner y JMX desactivados, H2 embebida (sin espera de contenedor de BD externo).
2. **Latencia**: la consulta de precio vigente usa el índice `(product_id, init_date, end_date)` y una *projection* (`SELECT p.value`) que evita materializar la entidad completa; igual para el historial.
3. **JVM / recursos** (`Dockerfile`): `-XX:MaxRAMPercentage=75.0` (heap acotado al límite del contenedor, dejando margen para metaspace/stacks), `-XX:+UseSerialGC` (con 1 CPU, un colector concurrente compite por el único core; con heaps pequeños las pausas de Serial GC son cortas), `-XX:+ExitOnOutOfMemoryError`.
4. **Virtual threads** (`spring.threads.virtual.enabled: true`): cada request en su propio hilo virtual, sin necesidad de dimensionar el pool de Tomcat para cargas I/O-bound.
5. **Imagen Docker**: multi-stage — build con `gradle:8.5.0-jdk21`, extracción de capas con Spring Boot layertools (`-Djarmode=tools`) y runtime en `eclipse-temurin:21-jre-alpine` (solo JRE, sin JDK/herramientas de build). Las capas se copian por separado para que Docker cachee la capa de dependencias, la que menos cambia entre builds.

Medido en un contenedor real con el límite de 1 CPU / 1 GB de `docker-compose.yml`:

| Métrica | Valor |
|---------|-------|
| Arranque | ~6.4 s |
| `GET` precio vigente | p50 2.7 ms / p95 4.9 ms |
| `GET` historial | p50 2.7 ms / p95 4.1 ms |
| `POST` producto | p50 2.1 ms / p95 3.0 ms |
| Memoria en reposo | ~276 MiB |
| Tamaño de imagen | ~261 MB |

No se añadió caché in-process (Caffeine): con estas latencias no estaba justificada; queda documentada como mejora posible si el patrón de acceso cambia (ver más abajo).

## Testing

- **Unitarios** (`src/test/.../service`, `domain`, `api`, `repository`): lógica de solapamiento y validaciones de `PriceService`/`ProductService`, entidades de dominio, DTOs y controllers con Mockito.
- **Integración** (`src/test/.../integration`, `@SpringBootTest` + `MockMvc`): los 4 endpoints, casos borde (`endDate` null, fechas límite inclusivas, solapamientos parciales/contenidos/con rango abierto), y respuestas `400`/`404`/`409`.
- **Concurrencia** (`PriceConcurrencyIntegrationTest`): 8 inserts concurrentes solapados sobre el mismo producto vía `TestRestTemplate` + puerto aleatorio, verificando que el lock pesimista deja pasar exactamente un `201`.

```bash
./gradlew test
```

## Benchmark de carga (k6)

`benchmark.sh` se ejecuta automáticamente vía `docker-compose.yml` en un contenedor auxiliar (`Dockerfile.benchmark`) después de que la API pasa su healthcheck. Tiene dos partes:

1. **Smoke test funcional** con `curl`: crea un producto, agrega tres precios (con y sin `endDate`) y consulta precio vigente/historial, para verificar que los 4 endpoints responden correctamente antes de someterlos a carga.
2. **Carga con k6** (`k6/load-test.js`) contra el producto ya sembrado, con una mezcla de tráfico 80% `GET` precio vigente / 15% `GET` historial / 5% `POST` producto, rampa de hasta 50 VUs concurrentes durante 70 s, y *thresholds* explícitos: `http_req_failed` < 1% y `p95 < 200 ms` / `p99 < 500 ms` (más finos por endpoint). Si algún threshold falla, `k6` devuelve código de salida distinto de cero y `benchmark.sh` propaga el fallo.

El contenedor `benchmark` es auxiliar (no la app bajo prueba), por lo que está limitado a 1 GB / 500 mCPU en `docker-compose.yml`, respetando la restricción del enunciado; los límites del contenedor `app` (1 CPU / 1 GB) no se tocan.

```bash
docker compose up --build
# los logs del contenedor `product-benchmark` muestran el resumen de k6 (latencias, thresholds, tasa de error)
```

Medido en local (Docker Desktop, límites del compose): 0% de errores sobre ~465k requests, p95 ≈ 29 ms / p99 ≈ 58 ms, muy por debajo de los thresholds configurados.

## Supuestos y desviaciones del enunciado

- Los endpoints `GET /products/{id}/prices` (con y sin `date`) comparten ruta y se diferencian por el query param `date`, tal como pide el enunciado para no romper las pruebas automáticas. Una alternativa más semántica sería `GET /products/{id}/prices/current?date=...`, mencionada aquí pero no aplicada.
- `value` se modela como `DECIMAL(12,2)` / `BigDecimal`, con `CHECK (value >= 0)` a nivel de esquema — el enunciado no aclara si se permiten precios negativos o cero; se asumió que no.
- No hay autenticación ni multi-moneda (fuera del alcance obligatorio).
- H2 en memoria implica que los datos no persisten entre reinicios del contenedor; es una decisión deliberada de rendimiento, no una limitación técnica (ver tabla de decisiones).

## Mejoras posibles

Priorizadas pero no implementadas por tiempo:

- `CommandLineRunner` condicional para poblar datos de prueba.
- Paginación/ordenamiento del historial de precios (`Pageable`), manteniendo el contrato por defecto.
- Caché Caffeine `(productId, date) → value` con invalidación al escribir, solo si un benchmark real lo justifica.
- Endpoints de actualización/borrado de precios.

## Estructura del proyecto

```
com.mango.products
├── ProductsApplication.java
├── api/            controllers, DTOs de request/response, GlobalExceptionHandler
├── domain/         entidades Product, Price
├── repository/     ProductRepository, PriceRepository
└── service/        ProductService, PriceService, excepciones de dominio
```

---

<details>
<summary><strong>Enunciado original de la prueba</strong></summary>

## 🧩 Contexto

Tu objetivo es diseñar e implementar una API que permita gestionar productos y sus precios históricos. Cada producto puede tener múltiples precios a lo largo del tiempo, pero solo un precio puede estar vigente para una misma fecha.

## 🎯 Objetivo

Queremos que demuestres tus conocimientos técnicos, tu criterio para tomar decisiones de diseño, y tu capacidad para resolver un problema realista de backend.

Puedes usar el **framework que prefieras**, la **arquitectura que consideres apropiada** y la **base de datos que mejor se adapte a tu solución**. Algunas opciones válidas incluyen Spring Boot, Quarkus, Java puro, PostgreSQL, MongoDB, MySQL, H2, etc.

La implementación puede realizarse en **Java o Kotlin**.

⚠️ **Uno de los requisitos más importantes de esta prueba es que tu solución tenga el mejor rendimiento posible**, tanto en tiempo de respuesta como en uso eficiente de recursos.

## 📘 Requisitos funcionales

### Endpoints obligatorios

Debes implementar los siguientes endpoints:

1. **Crear un producto**
    - `POST /products`
    - Body:
      ```json
      {
        "name": "Zapatillas deportivas",
        "description": "Modelo 2025 edición limitada"
      }
      ```

2. **Agregar un precio a un producto**
    - `POST /products/{id}/prices`
    - Body:
      ```json
      {
        "value": 99.99,
        "initDate": "2024-01-01",
        "endDate": "2024-06-30"
      }
      ```
    - Reglas:
        - No debe haber solapamiento de fechas con otros precios del mismo producto.
        - `endDate` puede ser `null`.
        - Validar que `initDate` < `endDate` si ambas existen.

3. **Obtener el precio vigente de un producto en una fecha**
    - `GET /products/{id}/prices?date=2024-04-15`
    - Body:
      ```json
      {
        "value": 99.99
      }
      ```

4. **Obtener el historial completo de precios de un producto**
    - `GET /products/{id}/prices`
    - Body:
      ```json
      {
        "name": "Zapatillas deportivas",
        "description": "Modelo 2025 edición limitada",
        "prices": [
          {
            "value": 99.99,
            "initDate": "2024-01-01",
            "endDate": "2024-06-30"
          },
          {
            "value": 199.99,
            "initDate": "2025-01-01",
            "endDate": "2025-06-30"
          },
        ]
      }
      ```

📌 **Nota**:
Los endpoints anteriores se utilizarán en las pruebas automáticas.
Sin embargo, **si consideras que alguno puede mejorarse para alinearse mejor con la semántica REST**, puedes hacerlo libremente, justificándolo en el README de tu proyecto.

## ✅ Criterios de evaluación

- Modelado correcto de entidades y relaciones.
- Validación robusta de reglas de negocio.
- Diseño RESTful claro y consistente.
- Organización del código y buenas prácticas.
- Elección justificada del stack técnico.
- **Rendimiento**: arranque rápido, respuestas ágiles, bajo uso de recursos.
- Tests automatizados (unitarios o de integración).
- Claridad en la documentación y facilidad de ejecución.

## 🚀 Desafíos opcionales (bonus)

### 1. Prueba de rendimiento automatizada

Puedes incluir una prueba automática de performance para validar el comportamiento de tu API bajo carga.

#### ¿Qué debes entregar?

- Un archivo `docker-compose.yml` que:
    - Levante tu aplicación.
    - Ejecute un script o herramienta (por ejemplo, Gatling, k6, Artillery, JMeter, etc.) con múltiples peticiones concurrentes.

#### ¿Qué se evaluará?

- Tiempo de arranque de la aplicación.
- Velocidad de ejecución de los endpoints.
- Peticiones exitosas por segundo.
- Uso de recursos bajo carga.

#### Restricciones importantes:

- **No se podrán modificar los valores de CPU ni memoria del contenedor de la aplicación ni del script de rendimiento**.
- **Puedes añadir nuevos contenedores auxiliares**, siempre que **cada uno tenga un máximo de 1 GB de memoria y 500 Mi de CPU**.

Esto te permite aplicar estrategias como separación de servicios, caché, balanceo, precálculo, etc., **pero dentro de restricciones razonables de infraestructura**.

### 2. Otros desafíos opcionales

- Soporte para múltiples monedas por precio.
- Endpoint para actualizar o eliminar precios.
- Autenticación básica o con token.
- Documentación con Swagger/OpenAPI.
- Scripts para poblar datos de prueba automáticamente.
- Soporte para paginación, ordenamiento o filtrado en el historial de precios.

## 📦 Entrega

### El `README.md` debe incluir:

- Instrucciones para compilar y ejecutar el proyecto.
- Justificación de decisiones técnicas.
- Indicaciones si agregaste mejoras, asumiste supuestos o cambiaste los endpoints.
- Cómo ejecutar la prueba de rendimiento (si aplicaste ese desafío).
- Para evitar copias preferimos que nos mandes un zip o nos envíes invitación de un repositorio PRIVADO de Github al contacto que te pasó la prueba.

</details>

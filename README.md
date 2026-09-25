# Franchise Management API
 
API reactiva para la gestión de franquicias, sucursales y productos.
Construida con Spring WebFlux y DynamoDB bajo arquitectura limpia, desplegable
en AWS ECS Fargate mediante Terraform.
 
---
 
## Tabla de contenidos
 
- [Stack](#stack)
- [Requisitos previos](#requisitos-previos)
- [Ejecución local](#ejecución-local)
- [API y Swagger](#api-y-swagger)
- [Pruebas](#pruebas)
- [Arquitectura](#arquitectura)
- [Modelo de datos](#modelo-de-datos)
---
 
## Stack
 
| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring WebFlux | reactivo |
| DynamoDB | AWS SDK v2 Enhanced Client (async) |
| Resilience4j | 2.4.0 |
| LocalStack | 4.4.0 (entorno local) |
| Terraform | >= 1.6 |
| Maven | wrapper incluido |
 
---
 
## Requisitos previos
 
| Herramienta | Versión mínima | Para qué |
|---|---|---|
| Docker | 20.10 | Levantar LocalStack y la aplicación |
| Docker Compose | v2 | Orquestar el entorno local |
| AWS CLI | v2 | Crear la tabla y desplegar |
| Java JDK | 21 | Solo si se ejecuta fuera de Docker |
| Terraform | >= 1.6 | Solo para el despliegue en AWS |
 
Maven no hace falta: el proyecto incluye el wrapper (`./mvnw`).
 
```bash
docker --version
docker compose version
aws --version      # debe decir aws-cli/2.x
```
 
---
 
## Ejecución local
 
### 1. Clonar
 
```bash
git clone https://github.com/juanchoy90/franchise-management.git
cd franchise-management
```
 
### 2. Levantar el entorno
 
```bash
docker compose up --build -d
```
 
Esto levanta LocalStack (DynamoDB en el puerto **4566**) y la aplicación
(puerto **8080**). La aplicación espera a que LocalStack esté listo antes de
arrancar.
 
Verifica que LocalStack responde:
 
```bash
curl http://localhost:4566/_localstack/health
```
 
`dynamodb` debe aparecer como `available`.
 
### 3. Crear la tabla
 
LocalStack arranca sin tablas. Este paso es obligatorio o la aplicación fallará
en la primera petición.
 
```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-1
 
aws dynamodb create-table \
  --table-name FranchiseManagement \
  --endpoint-url http://localhost:4566 \
  --attribute-definitions \
      AttributeName=PK,AttributeType=S \
      AttributeName=SK,AttributeType=S \
      AttributeName=GSI1PK,AttributeType=S \
      AttributeName=stock,AttributeType=N \
  --key-schema \
      AttributeName=PK,KeyType=HASH \
      AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes '[
    {
      "IndexName": "GSI1",
      "KeySchema": [
        {"AttributeName": "GSI1PK", "KeyType": "HASH"},
        {"AttributeName": "stock",  "KeyType": "RANGE"}
      ],
      "Projection": {
        "ProjectionType": "INCLUDE",
        "NonKeyAttributes": ["name", "nameKey", "id", "branchId", "franchiseId"]
      }
    }
  ]' \
  --billing-mode PAY_PER_REQUEST
```
 
Confirma:
 
```bash
aws dynamodb list-tables --endpoint-url http://localhost:4566
```
 
> **El atributo `stock` debe ser de tipo `N`.** Como `S`, el índice ordenaría
> lexicográficamente y `"9"` quedaría por encima de `"100"`.
 
> **LocalStack (edición community) no persiste datos entre reinicios.** Tras un
> `docker compose down` hay que recrear la tabla. Para reiniciar conservándola,
> usa `docker compose stop` y `docker compose start`.
 
### 4. Comprobar
 
```bash
curl http://localhost:8080/actuator/health
```
 
Debe devolver `{"status":"UP"}`.
 
### Ejecutar fuera de Docker
 
Con LocalStack ya levantado:
 
```bash
docker compose up -d localstack
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
 
En Windows:
 
```powershell
.\mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=local"
```
 
### Inspeccionar los datos
 
Se puede conectar **NoSQL Workbench** de AWS a `http://localhost:4566` con las
credenciales `test` / `test` para ver los ítems de la tabla.
 
---
 
## API y Swagger
 
| Recurso | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
 
La UI agrupa las operaciones en **Franchises**, **Branches** y **Products**.
Cada operación trae body de ejemplo, query params y respuestas 2xx / 4xx / 5xx.
 
Para probar desde el navegador: **Try it out** → rellenar el JSON o los query
params → **Execute**. El orden natural es franquicia → sucursal → producto.
Copia el `id` de cada `201` al siguiente request.
 
Prefijo `/v1`. Sucursales y productos son recursos top-level: el padre va en el
body (POST) o en query (`franchiseId`, `branchId`).
 
### Operaciones
 
| Tag | Método | Ruta | Body / query |
|---|---|---|---|
| Franchises | `POST` | `/v1/franchises` | `{"name":"McDonald's"}` |
| Franchises | `GET` | `/v1/franchises/{id}` | — |
| Franchises | `PATCH` | `/v1/franchises/{id}` | `{"name":"Popeyes"}` |
| Franchises | `GET` | `/v1/franchises/{id}/products/top-stock` | — |
| Branches | `POST` | `/v1/branches` | `{"franchiseId":"<fid>","name":"Downtown"}` |
| Branches | `PATCH` | `/v1/branches/{id}?franchiseId=` | `{"name":"Airport"}` |
| Products | `POST` | `/v1/products` | `{"franchiseId":"<fid>","branchId":"<bid>","name":"Fries","stock":10}` |
| Products | `PATCH` | `/v1/products/{id}?franchiseId=&branchId=` | `{"delta":15}` |
| Products | `PATCH` | `/v1/products/{id}/name?franchiseId=&branchId=` | `{"name":"Burger"}` |
| Products | `DELETE` | `/v1/products/{id}?franchiseId=&branchId=` | — → `204` |
 
El nombre de producto es **único dentro de la sucursal**; el mismo nombre en
otra sucursal está permitido. El stock se ajusta por **delta**, no por valor
absoluto.
 
### Flujo de prueba completo
 
Requiere [`jq`](https://jqlang.github.io/jq/) para encadenar los ids.
 
```bash
BASE=http://localhost:8080/v1
 
FID=$(curl -s -X POST $BASE/franchises \
  -H "Content-Type: application/json" \
  -d '{"name":"McDonalds"}' | jq -r '.id')
 
BID=$(curl -s -X POST $BASE/branches \
  -H "Content-Type: application/json" \
  -d "{\"franchiseId\":\"$FID\",\"name\":\"Downtown\"}" | jq -r '.id')
 
PID=$(curl -s -X POST $BASE/products \
  -H "Content-Type: application/json" \
  -d "{\"franchiseId\":\"$FID\",\"branchId\":\"$BID\",\"name\":\"Fries\",\"stock\":10}" | jq -r '.id')
 
curl -s -X POST $BASE/products \
  -H "Content-Type: application/json" \
  -d "{\"franchiseId\":\"$FID\",\"branchId\":\"$BID\",\"name\":\"Nuggets\",\"stock\":100}" > /dev/null
 
# Producto con mayor stock por sucursal
curl -s "$BASE/franchises/$FID/products/top-stock" | jq
 
# Ajustar stock por delta
curl -s -X PATCH "$BASE/products/$PID?franchiseId=$FID&branchId=$BID" \
  -H "Content-Type: application/json" -d '{"delta":-3}' | jq
 
# Renombrar
curl -s -X PATCH "$BASE/franchises/$FID" \
  -H "Content-Type: application/json" -d '{"name":"Popeyes"}' | jq
 
# Eliminar producto
curl -s -o /dev/null -w "%{http_code}\n" \
  -X DELETE "$BASE/products/$PID?franchiseId=$FID&branchId=$BID"
```
 
### Respuestas de ejemplo
 
Crear franquicia (`201`):
 
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "McDonald's",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```
 
Top stock por sucursal (`200`). Una sucursal sin productos lleva `product: null`:
 
```json
[
  {
    "branchId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "branchName": "Downtown",
    "product": { "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "name": "Fries", "stock": 40 }
  },
  {
    "branchId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "branchName": "Airport",
    "product": null
  }
]
```
 
### Errores
 
Todos los fallos usan el mismo JSON. El `traceId` también viaja en el header
`X-Trace-Id` (se puede enviar uno propio o la API genera uno).
 
```json
{
  "code": "FRANCHISE_ALREADY_EXISTS",
  "message": "A franchise with that name already exists",
  "path": "/v1/franchises",
  "timestamp": "2026-01-01T00:00:00Z",
  "traceId": "8f3c1d2e-4b5a-6789-abcd-ef0123456789"
}
```
 
| HTTP | Código |
|---|---|
| `400` | `VALIDATION_ERROR`, `INVALID_STOCK`, `STOCK_DELTA_OUT_OF_RANGE` |
| `404` | `FRANCHISE_NOT_FOUND`, `BRANCH_NOT_FOUND`, `PRODUCT_NOT_FOUND` |
| `409` | `FRANCHISE_ALREADY_EXISTS`, `BRANCH_ALREADY_EXISTS`, `PRODUCT_ALREADY_EXISTS`, `INSUFFICIENT_STOCK` |
| `429` | `SERVICE_THROTTLED` |
| `503` | `SERVICE_UNAVAILABLE` |
| `504` | `TIMEOUT` |
 
---
 
## Pruebas
 
```bash
./mvnw test              # unitarias
./mvnw verify            # todo, incluida la verificación de cobertura
```
 
Reporte de cobertura en `target/site/jacoco/index.html`.
 
| Tipo | Herramienta | Qué valida |
|---|---|---|
| Unitarias | JUnit 5 + Mockito + StepVerifier | Casos de uso y mappers sin infraestructura |
| Arquitectura | ArchUnit | Que el dominio no importe Spring ni el SDK de AWS |
| Integración | Testcontainers + LocalStack | Adaptadores contra un DynamoDB real |
| Resiliencia | Testcontainers + Toxiproxy | Que el circuit breaker y el retry reaccionen ante latencia y cortes de red |
 
Las pruebas de integración requieren Docker en ejecución.
 
---
 
## Arquitectura
 
Arquitectura limpia con separación estricta de capas.
 
```
src/main/java/co/com/juandavidg/
├── domain/
│   ├── model/              # Entidades y excepciones. Sin frameworks.
│   │   ├── franchise/
│   │   ├── branch/
│   │   ├── product/
│   │   └── exceptions/     # DomainException, BusinessException, TechnicalException
│   └── usecase/            # Reglas de negocio. Sin anotaciones de Spring.
└── infrastructure/
    ├── driven-adapters/
    │   └── dynamodb/       # Entidades de persistencia, mappers, adaptadores
    └── entry-points/
        └── api/            # Handlers, routers, manejo global de errores
```
 
**La regla:** las dependencias apuntan hacia adentro. El dominio no conoce
Spring, ni DynamoDB, ni HTTP. Las reglas de ArchUnit lo verifican en cada
build, así que la frontera no depende de la disciplina de quien escribe.
 
Ninguna excepción del SDK de AWS ni de Resilience4j sale del módulo del
adaptador: se traducen a `BusinessException` o `TechnicalException` antes de
llegar al caso de uso. El `ErrorCode` del dominio **no contiene `HttpStatus`**;
esa traducción vive en el entry-point, para que el mismo caso de uso pueda
exponerse mañana por otro transporte.
 
---
 
## Modelo de datos
 
Diseño single-table: toda la jerarquía de una franquicia vive en la misma
partición, de modo que traerla completa es un solo `Query`.
 
| Ítem | PK | SK |
|---|---|---|
| Franquicia | `FRANCHISE#<fid>` | `#META` |
| Sucursal | `FRANCHISE#<fid>` | `BRANCH#<bid>` |
| Producto | `FRANCHISE#<fid>` | `PRODUCT#<bid>#<pid>` |
| Lock nombre sucursal | `FRANCHISE#<fid>` | `UNIQ#BRANCH#<nameKey>` |
| Lock nombre producto | `FRANCHISE#<fid>` | `UNIQ#PRODUCT#<bid>#<nameKey>` |
| Lock nombre franquicia | `FRANCHISE#NAME#<nameKey>` | `UNIQUE` |
 
El orden de la sort key (`#META` → `BRANCH#` → `PRODUCT#` → `UNIQ#`) agrupa cada
tipo y permite consultas por prefijo sin traer los demás.
 
### Índice GSI1
 
```
GSI1PK = BRANCH#<franchiseId>#<branchId>
stock  = sort key (tipo Number)
```
 
Índice **disperso**: solo los productos escriben `GSI1PK`, así que contiene
exactamente productos. `stock` es a la vez dato del producto y llave de
ordenamiento, de modo que no hay dos atributos que mantener sincronizados.
 
Obtener el producto con mayor stock de una sucursal es un `Query` con
`scanIndexForward(false)` y `limit(1)`: **un ítem leído**, sin importar cuántos
productos tenga la sede.
 
### Unicidad de nombres
 
| Alcance | Dónde |
|---|---|
| Franquicia | Global |
| Sucursal | Dentro de su franquicia |
| Producto | Dentro de su sucursal |
 
Se garantiza con **ítems lock escritos en `TransactWriteItems`** con
`attribute_not_exists`. No se usa "consultar y luego escribir": eso deja una
ventana en la que dos peticiones simultáneas pasan ambas la verificación.
 
Los nombres se normalizan antes de comparar (minúsculas, sin tildes, espacios
colapsados) y el nombre original se conserva aparte para mostrar.


## Despliegue en AWS

Infraestructura completa en `terraform/`. Ver [terraform/README.md](terraform/README.md)
para el detalle de cada módulo.

### Arquitectura desplegada

```
Internet → ALB (subredes públicas)
             ↓
        ECS Fargate (subredes privadas, auto scaling)
             ↓
        DynamoDB (vía VPC Gateway Endpoint)
```

### Pasos

```bash

./scripts/bootstrap.sh


# 2. Desplegar
./scripts/deploy-env.sh dev

# 3. Destruir
./scripts/destroy.sh dev
```

El paso 2 crea toda la infraestructura, construye la imagen, la publica en ECR
y despliega el servicio. Tarda unos 10 minutos la primera vez.

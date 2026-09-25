# Infraestructura — Franchise Management API
 
Terraform modular para desplegar la aplicación en ECS Fargate detrás de un ALB,
con imagen en ECR, secretos en Secrets Manager y estado remoto en S3.
 
---
 
## Estructura
 
```
terraform/
├── bootstrap/              # Backend remoto (se aplica UNA vez, estado local)
├── modules/
│   ├── networking/         # VPC, subredes, NAT, VPC endpoint, security groups
│   ├── ecr/                # Repositorio de imágenes + lifecycle policy
│   ├── dynamodb/           # Tabla single-table + GSI1
│   ├── secrets/            # Contenedor del secreto (nunca el valor)
│   ├── iam/                # Roles de ejecución, tarea y autoscaling
│   ├── alb/                # Balanceador, target group, listeners
│   ├── ecs-service/        # Cluster, task definition, servicio, autoscaling
│   ├── github-oidc/        # Rol para GitHub Actions sin claves de AWS
│   └── platform/           # Composición: cablea todo lo anterior
└── environments/
    ├── dev/
    ├── staging/
    └── prod/
```
 
## Arquitectura desplegada
 
```
Internet → ALB (subredes públicas)
             ↓
        ECS Fargate (subredes privadas, auto scaling)
             ↓
        DynamoDB (vía VPC Gateway Endpoint)
```
 
Las tareas nunca viven en subred pública: solo el ALB es alcanzable desde
internet, y el security group de las tareas únicamente acepta tráfico
proveniente del security group del ALB.
 
DynamoDB es un servicio regional, no vive dentro de la VPC. El equivalente a
"base de datos en subred privada" es el **VPC Gateway Endpoint**: el tráfico
hacia DynamoDB viaja por la red interna de AWS sin salir a internet ni pasar
por el NAT. Además es gratuito y reduce la transferencia facturable.
 
---
 
## Por qué esta estructura
 
**Módulos de infraestructura** (`networking`, `ecs-service`, …) son piezas
reutilizables que no saben nada del entorno. Reciben todo por variable.
 
**Módulo de composición** (`platform`) cablea las piezas entre sí. Existe para
que el cableado viva una sola vez: sin él, los tres `environments/*/main.tf`
repetirían las mismas ~150 líneas y un cambio arquitectónico obligaría a editar
tres archivos idénticos.
 
**Directorios por entorno** en lugar de workspaces. Cada uno tiene su propio
`key` en el backend, así que dev no puede corromper el estado de prod y un
`destroy` mal dirigido afecta a un solo entorno. Los workspaces comparten
backend y hacen más fácil aplicar en el entorno equivocado. El intercambio:
algo de duplicación en los `main.tf` a cambio de aislamiento.
 
---
 
## Despliegue
 
### 1. Backend remoto (una sola vez)
 
```bash
cd bootstrap
terraform init
terraform apply
```
 
Copia el `state_bucket` del output al `backend.tf` de cada entorno,
reemplazando `CAMBIAR_POR_ACCOUNT_ID`.
 
El backend no puede guardar su propio estado en un bucket que todavía no
existe, por eso se aplica con estado local. Su `terraform.tfstate` **sí se
versiona en Git**: es la única excepción, y es lo que permite reproducir el
backend si se pierde.
 
### 2. Infraestructura
 
```bash
cd environments/dev
terraform init
terraform plan
terraform apply
```
 
### 3. Cargar los secretos
 
Terraform crea el contenedor del secreto con placeholders; los valores se
cargan fuera:
 
```bash
aws secretsmanager put-secret-value \
  --secret-id franchise/dev/app \
  --secret-string '{"CLAVE":"valor"}'
```
 
### 4. Publicar la imagen
 
```bash
ECR_URL=$(terraform output -raw ecr_repository_url)
 
aws ecr get-login-password | docker login --username AWS --password-stdin "${ECR_URL%/*}"
 
docker build --platform linux/amd64 --provenance=false --sbom=false \
  -t "$ECR_URL:v1.0.0" .
docker push "$ECR_URL:v1.0.0"
 
terraform apply -var="image_tag=v1.0.0"
```
 
Las banderas `--provenance=false --sbom=false` son obligatorias: sin ellas,
Docker con BuildKit genera un manifest list con attestations que ECS no puede
resolver, y el despliegue falla con `CannotPullContainerError: not found`
aunque la imagen aparezca en ECR.
 
---
 
## Gestión del estado
 
El estado vive en S3 con versionado habilitado y bloqueo en DynamoDB:
 
```
s3://franchise-tfstate-<account-id>/
├── dev/terraform.tfstate
├── staging/terraform.tfstate
└── prod/terraform.tfstate
```
 
| Mecanismo | Para qué |
|---|---|
| Un `key` por entorno | Aislamiento: un `destroy` en dev no puede alcanzar prod |
| Bloqueo en DynamoDB | Impide que dos `apply` simultáneos corrompan el estado |
| Versionado del bucket | Permite recuperar un estado corrupto o revertir |
| Cifrado en reposo | El estado contiene valores sensibles en texto plano |
| `prevent_destroy` | El bucket y la tabla de bloqueo no se borran por accidente |
 
---
 
## Gestión de secretos
 
El principio: **Terraform crea el contenedor, nunca el valor**.
 
Poner un valor sensible en un `.tf` lo deja en texto plano en el repositorio y,
peor, en el estado remoto, que no está cifrado a nivel de campo. Cualquiera con
acceso de lectura al bucket lo vería.
 
En la definición de tarea hay dos bloques distintos:
 
| Bloque | Contenido | Visibilidad |
|---|---|---|
| `environment` | Configuración no sensible (nombre de tabla, región) | Texto plano en la consola de ECS |
| `secrets` | Referencias `valueFrom` a Secrets Manager | El valor nunca sale del contenedor |
 
ECS resuelve cada `valueFrom` en el arranque usando el rol de ejecución. El
valor no aparece en la definición de tarea, ni en la consola, ni en el estado.
 
El recurso lleva `ignore_changes = [secret_string]` para que un `apply`
posterior no sobrescriba con el placeholder el valor cargado entre medias. Eso
es idempotencia real: aplicar dos veces no destruye nada.
 
---
 
## Roles IAM
 
Son dos roles distintos y confundirlos es el error más común:
 
| Rol | Quién lo usa | Para qué |
|---|---|---|
| `execution_role` | El agente de ECS, antes de arrancar el contenedor | Bajar la imagen de ECR, resolver secretos, crear el log stream |
| `task_role` | El código de la aplicación, en runtime | Hablar con DynamoDB |
 
Meter los permisos de DynamoDB en el `execution_role` rompe el mínimo
privilegio: el agente no los necesita y el código no necesita bajar imágenes.
 
Restricciones aplicadas:
 
- Cada acción se acota al ARN exacto del recurso de ese entorno. **El rol de
  dev no puede leer el secreto de prod.**
- Las acciones están enumeradas: no hay `dynamodb:*`.
- La política de DynamoDB no incluye `Scan`, `CreateTable` ni `DeleteTable`. Si
  apareciera un `Scan` en el código, fallaría en runtime y sería señal de que
  falta un índice.
- `ecr:GetAuthorizationToken` es la única acción con `Resource: "*"`, porque la
  API de AWS no admite restricción por recurso en esa acción. Está aislada en
  su propio statement y documentada.
- El `assume_role_policy` incluye una condición `aws:SourceAccount` contra el
  ataque de *confused deputy*.
- ECS Exec (shell dentro del contenedor) solo se habilita fuera de producción.
---
 
## Escalamiento
 
Tres políticas de target tracking sobre el mismo servicio:
 
| Métrica | Objetivo | Por qué |
|---|---|---|
| CPU | 70% (60% en prod) | La base |
| Memoria | 75% (70% en prod) | La JVM puede saturar memoria sin saturar CPU |
| Peticiones por tarea | 1000 (solo prod) | En WebFlux la CPU se mantiene baja bajo carga de I/O, así que escalar solo por CPU reacciona tarde |
 
`scale_in_cooldown` (300 s) es más alto que `scale_out_cooldown` (60 s): subir
rápido y bajar despacio evita el flapping de tareas ante picos cortos.
 
El `desired_count` del servicio está en `ignore_changes`. Sin eso, cada `apply`
devolvería el conteo al valor inicial y desharía el escalado en curso.
 
---
 
## Diferencias entre entornos
 
| | dev | staging | prod |
|---|---|---|---|
| NAT Gateway | 1 compartido | 1 por AZ | 1 por AZ |
| AZs | 2 | 2 | 3 |
| CPU / memoria | 256 / 512 | 512 / 1024 | 1024 / 2048 |
| Tareas (mín–máx) | 1–3 | 2–6 | 3–20 |
| Capacidad | 100% Spot | Mixto | 100% Fargate |
| Retención de logs | 7 días | 30 días | 90 días |
| ECS Exec | ✓ | ✓ | ✗ |
| Protección de borrado | ✗ | ✗ | ✓ |
| PITR en DynamoDB | ✗ | ✓ | ✓ |
| Streams | ✗ | ✗ | ✓ |
| `image_tag` | `latest` permitido | `latest` permitido | Rechazado |
 
---
 
## Principios de IaC aplicados
 
**Reproducibilidad.** Los tres entornos ejecutan el mismo código; solo cambian
los parámetros. `required_version` y `~> 5.60` en el provider fijan las
versiones para que el mismo código produzca la misma infraestructura dentro de
seis meses. El `.terraform.lock.hcl` se versiona y fija los hashes exactos.
 
**Idempotencia.** Aplicar dos veces no cambia nada la segunda. El caso que lo
pone a prueba es el secreto, resuelto con `ignore_changes`.
 
**Versionamiento.** Estado versionado en S3, imágenes inmutables en prod
(`IMMUTABLE` en ECR) y `image_tag` de producción que rechaza `latest`: sin eso
no se sabe qué versión corre ni a cuál revertir.
 
---
 
## CI/CD sin claves de AWS
 
El módulo `github-oidc` crea un rol que GitHub Actions asume mediante OIDC. La
alternativa sería guardar un `AWS_ACCESS_KEY_ID` en los secrets de GitHub: una
credencial de larga vida, en un sistema de terceros, que no rota.
 
Con OIDC, GitHub presenta un token firmado de vida corta y AWS entrega
credenciales temporales. No hay ninguna clave que guardar.
 
La condición `sub` del rol restringe qué repositorio y qué ramas pueden
asumirlo. **Sin ella, cualquier repositorio de GitHub en el mundo podría
hacerlo.**
 
---
 
## Costos
 
| Recurso | Aproximado |
|---|---|
| NAT Gateway | ~32 USD/mes cada uno, más tráfico |
| ALB | ~16 USD/mes, más LCU |
| Fargate (1 tarea, 0.25 vCPU / 0.5 GB) | ~9 USD/mes |
| DynamoDB on-demand | centavos con tráfico de prueba |
| ECR, Secrets Manager, CloudWatch | ~1-2 USD/mes |
 
Dev con un solo NAT ronda los **55-60 USD/mes**. Ejecutar el `destroy` al
terminar; recrear el entorno idéntico toma unos 10 minutos, que es justamente
la reproducibilidad que el modelo persigue.
 
Para bajar a ~25 USD/mes se puede eliminar el NAT y poner las tareas en
subredes públicas con `assign_public_ip = true`: siguen protegidas porque el
security group solo acepta tráfico del ALB.
 
---
 
## Pendientes
 
1. **WAF delante del ALB.** Lo siguiente en prod, pero agrega costo fijo.
2. **Route 53 y certificado ACM.** El módulo acepta `certificate_arn`, pero
   crear el certificado requiere un dominio real.
3. **Proyección CQRS.** `dynamodb_stream_enabled` está activo en prod, que es
   el punto de enganche. La Lambda y el cluster de OpenSearch serían el
   siguiente módulo.
---
 
## Solución de problemas
 
| Síntoma | Causa | Solución |
|---|---|---|
| `CannotPullContainerError: not found` | Manifest list con attestations | Construir con `--platform linux/amd64 --provenance=false --sbom=false` |
| Las tareas arrancan y mueren | Health check antes de que la JVM termine de arrancar | Subir `health_check_grace_period`; revisar `/ecs/franchise-dev` |
| `unable to pull secrets` | El rol de ejecución no puede leer el secreto, o no existe | Verificar el paso 3 |
| La tarea no baja la imagen | Sin salida a internet | Ruta al NAT si está en subred privada, o `assign_public_ip` si es pública |
| `terraform destroy` falla en ECR | El repositorio contiene imágenes | `force_delete = true` en el módulo, o vaciarlo a mano |
| `RepositoryNotEmptyException` persistente | Attestation manifests huérfanos | `aws ecr delete-repository --force` |
 

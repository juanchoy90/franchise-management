
set -euo pipefail

ENV="${1:?Uso: $0 <dev|staging|prod> [tag]}"
TAG="${2:-$(git rev-parse --short HEAD)}"
REGION="${AWS_REGION:-us-east-1}"

cd "$(dirname "$0")/../terraform/environments/$ENV"

echo "==> Creando infraestructura ($ENV)"
terraform init
terraform apply -auto-approve -var="image_tag=$TAG"

ECR_URL=$(terraform output -raw ecr_repository_url)
CLUSTER=$(terraform output -raw ecs_cluster_name)
SERVICE=$(terraform output -raw ecs_service_name)

echo "==> Construyendo y publicando imagen $TAG"
cd ../../..
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ECR_URL%/*}"

docker build --platform linux/amd64 --provenance=false --sbom=false -t "$ECR_URL:$TAG" . \
  && docker push "$ECR_URL:$TAG"

echo "==> Forzando redespliegue"
aws ecs update-service \
  --cluster "$CLUSTER" --service "$SERVICE" \
  --force-new-deployment --region "$REGION" >/dev/null

aws ecs wait services-stable \
  --cluster "$CLUSTER" --services "$SERVICE" --region "$REGION"

cd "terraform/environments/$ENV"
URL=$(terraform output -raw application_url)
echo
echo "Listo: $URL"
curl -fsS "$URL/actuator/health" && echo

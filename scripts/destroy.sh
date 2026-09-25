set -euo pipefail

ENV="${1:?Uso: $0 <dev|staging|prod>}"

read -rp "Vas a DESTRUIR el entorno '$ENV'. Escribe el nombre para confirmar: " confirm
[ "$confirm" = "$ENV" ] || { echo "Cancelado"; exit 1; }

cd "$(dirname "$0")/../terraform/environments/$ENV"
terraform destroy

project_name       = "franchise"
environment        = "prod"
aws_region         = "us-east-1"
availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
vpc_cidr           = "10.30.0.0/16"

# image_tag NO se define aqui: lo inyecta el pipeline en cada despliegue
#   terraform apply -var="image_tag=v1.4.2"

# certificate_arn = "arn:aws:acm:us-east-1:...:certificate/..."

secret_keys = []

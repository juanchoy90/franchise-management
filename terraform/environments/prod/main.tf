provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = var.repository_url
      Criticality = "high"
    }
  }
}

locals {
  validate_image_tag = var.image_tag == null || var.image_tag == "latest" ? tobool("ERROR: en produccion image_tag debe ser una version inmutable, nunca 'latest'") : true
}

module "platform" {
  source = "../../modules/platform"

  project_name       = var.project_name
  environment        = var.environment
  aws_region         = var.aws_region
  availability_zones = var.availability_zones
  vpc_cidr           = var.vpc_cidr

  image_tag       = var.image_tag
  container_port  = var.container_port
  secret_keys     = var.secret_keys
  certificate_arn = var.certificate_arn

  single_nat_gateway = false

  task_cpu    = 1024
  task_memory = 2048

  desired_count = 3
  min_capacity  = 3
  max_capacity  = 20

  cpu_target_value    = 60
  memory_target_value = 70

  requests_per_target = 1000

  capacity_provider_strategy = [
    { capacity_provider = "FARGATE", weight = 100, base = 3 }
  ]

  log_retention_days        = 90
  enable_container_insights = true

  enable_execute_command = false

  enable_deletion_protection     = true
  secret_recovery_window_in_days = 30

  dynamodb_point_in_time_recovery = true

  dynamodb_stream_enabled = true
}

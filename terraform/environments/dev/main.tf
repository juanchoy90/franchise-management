provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = var.repository_url
    }
  }
}

module "platform" {
  source = "../../modules/platform"

  project_name       = var.project_name
  environment        = var.environment
  aws_region         = var.aws_region
  availability_zones = var.availability_zones
  vpc_cidr           = var.vpc_cidr

  image_tag      = var.image_tag
  container_port = var.container_port
  secret_keys    = var.secret_keys

  single_nat_gateway = true

  task_cpu      = 256
  task_memory   = 512
  desired_count = 1
  min_capacity  = 1
  max_capacity  = 3

  capacity_provider_strategy = [
    { capacity_provider = "FARGATE_SPOT", weight = 100, base = 0 }
  ]

  log_retention_days        = 7
  enable_container_insights = false

  enable_execute_command = true

  enable_deletion_protection     = false
  secret_recovery_window_in_days = 0

  dynamodb_point_in_time_recovery = false
  dynamodb_stream_enabled         = false
}

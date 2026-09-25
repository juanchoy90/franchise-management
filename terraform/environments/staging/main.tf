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

  single_nat_gateway = false

  task_cpu      = 512
  task_memory   = 1024
  desired_count = 2
  min_capacity  = 2
  max_capacity  = 6

  cpu_target_value    = 70
  memory_target_value = 75

  capacity_provider_strategy = [
    { capacity_provider = "FARGATE", weight = 1, base = 1 },
    { capacity_provider = "FARGATE_SPOT", weight = 3, base = 0 }
  ]

  log_retention_days        = 30
  enable_container_insights = true
  enable_execute_command    = true

  enable_deletion_protection     = false
  secret_recovery_window_in_days = 7

  dynamodb_point_in_time_recovery = true
  dynamodb_stream_enabled         = false
}

locals {
  name = "${var.project_name}-${var.environment}"
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.name}"
  retention_in_days = var.log_retention_days

  tags = { Name = "${local.name}-logs" }
}

module "networking" {
  source = "../networking"

  project_name       = var.project_name
  environment        = var.environment
  aws_region         = var.aws_region
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  single_nat_gateway = var.single_nat_gateway
  container_port     = var.container_port
  certificate_arn    = var.certificate_arn
}

module "ecr" {
  source = "../ecr"

  project_name    = var.project_name
  environment     = var.environment
  max_image_count = var.max_image_count
}

module "dynamodb" {
  source = "../dynamodb"

  project_name           = var.project_name
  environment            = var.environment
  billing_mode           = var.dynamodb_billing_mode
  point_in_time_recovery = var.dynamodb_point_in_time_recovery
  stream_enabled         = var.dynamodb_stream_enabled
}

module "secrets" {
  source = "../secrets"

  project_name            = var.project_name
  environment             = var.environment
  secret_keys             = var.secret_keys
  recovery_window_in_days = var.secret_recovery_window_in_days
}

module "iam" {
  source = "../iam"

  project_name           = var.project_name
  environment            = var.environment
  ecr_repository_arn     = module.ecr.repository_arn
  secret_arns            = [module.secrets.secret_arn]
  log_group_arn          = aws_cloudwatch_log_group.app.arn
  dynamodb_table_arn     = module.dynamodb.table_arn
  dynamodb_index_arns    = module.dynamodb.index_arns
  enable_execute_command = var.enable_execute_command
}

module "alb" {
  source = "../alb"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = module.networking.vpc_id
  public_subnet_ids          = module.networking.public_subnet_ids
  security_group_id          = module.networking.alb_security_group_id
  container_port             = var.container_port
  health_check_path          = var.health_check_path
  enable_deletion_protection = var.enable_deletion_protection
  certificate_arn            = var.certificate_arn
}

module "ecs" {
  source = "../ecs-service"

  project_name = var.project_name
  environment  = var.environment

  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.ecs_tasks_security_group_id

  ecr_repository_url = module.ecr.repository_url
  image_tag          = var.image_tag
  container_port     = var.container_port
  cpu_architecture   = var.cpu_architecture

  task_cpu    = var.task_cpu
  task_memory = var.task_memory

  desired_count       = var.desired_count
  min_capacity        = var.min_capacity
  max_capacity        = var.max_capacity
  cpu_target_value    = var.cpu_target_value
  memory_target_value = var.memory_target_value
  requests_per_target = var.requests_per_target

  target_group_arn        = module.alb.target_group_arn
  target_group_arn_suffix = module.alb.target_group_arn_suffix
  alb_arn_suffix          = module.alb.alb_arn_suffix

  execution_role_arn   = module.iam.execution_role_arn
  task_role_arn        = module.iam.task_role_arn
  autoscaling_role_arn = module.iam.autoscaling_role_arn

  log_group_name = aws_cloudwatch_log_group.app.name

  environment_variables = merge(
    {
      SPRING_PROFILES_ACTIVE     = var.environment
      AWS_REGION                 = var.aws_region
      DYNAMODB_TABLE_FRANCHISE   = module.dynamodb.table_name
      SERVER_PORT                = tostring(var.container_port)
      JAVA_TOOL_OPTIONS          = var.java_tool_options
    },
    var.extra_environment_variables
  )

  container_secrets = module.secrets.container_secrets

  health_check_path          = var.health_check_path
  capacity_providers         = var.capacity_providers
  capacity_provider_strategy = var.capacity_provider_strategy
  enable_container_insights  = var.enable_container_insights
  enable_execute_command     = var.enable_execute_command
}

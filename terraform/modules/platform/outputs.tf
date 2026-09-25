output "application_url" {
  description = "URL publica del servicio."
  value       = var.certificate_arn == null ? "http://${module.alb.alb_dns_name}" : "https://${module.alb.alb_dns_name}"
}

output "alb_dns_name" {
  description = "DNS del balanceador, para crear un alias en Route 53."
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "Zone ID del ALB."
  value       = module.alb.alb_zone_id
}

output "ecr_repository_url" {
  description = "Destino del docker push en el pipeline de CI."
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  description = "Cluster ECS, para forzar redespliegues desde CI."
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "Servicio ECS."
  value       = module.ecs.service_name
}

output "dynamodb_table_name" {
  description = "Tabla de la aplicacion."
  value       = module.dynamodb.table_name
}

output "dynamodb_stream_arn" {
  description = "ARN del stream, si esta habilitado. Punto de enganche para CQRS."
  value       = module.dynamodb.stream_arn
}

output "secret_name" {
  description = "Nombre del secreto. Los valores se cargan con la CLI de AWS."
  value       = module.secrets.secret_name
}

output "log_group_name" {
  description = "Log group de CloudWatch."
  value       = aws_cloudwatch_log_group.app.name
}

output "task_role_arn" {
  description = "Rol de la aplicacion en runtime."
  value       = module.iam.task_role_arn
}

output "vpc_id" {
  description = "VPC del entorno."
  value       = module.networking.vpc_id
}

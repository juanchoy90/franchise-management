output "application_url" {
  description = "URL del servicio en produccion."
  value       = module.platform.application_url
}

output "ecr_repository_url" {
  description = "Destino del docker push."
  value       = module.platform.ecr_repository_url
}

output "ecs_cluster_name" {
  value = module.platform.ecs_cluster_name
}

output "ecs_service_name" {
  value = module.platform.ecs_service_name
}

output "dynamodb_table_name" {
  value = module.platform.dynamodb_table_name
}

output "secret_name" {
  description = "Secreto donde cargar las credenciales con la CLI."
  value       = module.platform.secret_name
}

output "log_group_name" {
  value = module.platform.log_group_name
}

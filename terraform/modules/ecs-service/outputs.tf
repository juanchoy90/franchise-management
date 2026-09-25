output "cluster_name" {
  description = "Nombre del cluster ECS."
  value       = aws_ecs_cluster.this.name
}

output "cluster_arn" {
  description = "ARN del cluster."
  value       = aws_ecs_cluster.this.arn
}

output "service_name" {
  description = "Nombre del servicio. Util para forzar un redespliegue desde CI."
  value       = aws_ecs_service.this.name
}

output "task_definition_arn" {
  description = "ARN de la definicion de tarea, con su revision."
  value       = aws_ecs_task_definition.this.arn
}

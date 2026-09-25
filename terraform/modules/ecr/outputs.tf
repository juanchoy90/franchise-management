output "repository_url" {
  description = "URL del repositorio. Se usa como destino del docker push."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "ARN del repositorio. Lo necesita el rol de ejecucion de ECS."
  value       = aws_ecr_repository.this.arn
}

output "repository_name" {
  description = "Nombre del repositorio."
  value       = aws_ecr_repository.this.name
}

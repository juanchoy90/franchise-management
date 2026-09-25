output "table_name" {
  description = "Nombre de la tabla. Se inyecta al contenedor como variable de entorno."
  value       = aws_dynamodb_table.this.name
}

output "table_arn" {
  description = "ARN de la tabla. Lo usa la politica IAM del rol de tarea."
  value       = aws_dynamodb_table.this.arn
}

output "index_arns" {
  description = "ARNs de los indices. IAM los requiere aparte del ARN de la tabla."
  value       = ["${aws_dynamodb_table.this.arn}/index/*"]
}

output "stream_arn" {
  description = "ARN del stream, si esta habilitado."
  value       = aws_dynamodb_table.this.stream_arn
}

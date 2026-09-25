output "secret_arn" {
  description = "ARN del secreto. La politica IAM lo restringe a este ARN exacto."
  value       = aws_secretsmanager_secret.app.arn
}

output "secret_name" {
  description = "Nombre del secreto, para cargar valores con la CLI."
  value       = aws_secretsmanager_secret.app.name
}

output "container_secrets" {
  description = <<-EOT
    Lista lista para el bloque `secrets` de la definicion de tarea.
    ECS resuelve cada valueFrom en el arranque y lo inyecta como variable
    de entorno DENTRO del contenedor: el valor nunca aparece en la
    definicion de tarea ni en la consola.
  EOT
  value = [
    for key in var.secret_keys : {
      name      = key
      valueFrom = "${aws_secretsmanager_secret.app.arn}:${key}::"
    }
  ]
}

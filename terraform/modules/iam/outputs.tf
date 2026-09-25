output "execution_role_arn" {
  description = "Rol del agente ECS: baja la imagen y resuelve secretos."
  value       = aws_iam_role.execution.arn
}

output "task_role_arn" {
  description = "Rol de la aplicacion en runtime: acceso a DynamoDB."
  value       = aws_iam_role.task.arn
}

output "autoscaling_role_arn" {
  description = "Rol de Application Auto Scaling."
  value       = aws_iam_role.autoscaling.arn
}

output "vpc_id" {
  description = "Identificador de la VPC."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Subredes publicas, donde vive el ALB."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Subredes privadas, donde corren las tareas ECS."
  value       = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  description = "Security group del balanceador."
  value       = aws_security_group.alb.id
}

output "ecs_tasks_security_group_id" {
  description = "Security group de las tareas. Solo acepta trafico del ALB."
  value       = aws_security_group.ecs_tasks.id
}

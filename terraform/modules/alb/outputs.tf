output "alb_arn" {
  description = "ARN del balanceador."
  value       = aws_lb.this.arn
}

output "alb_dns_name" {
  description = "DNS publico del ALB. Es la URL del servicio."
  value       = aws_lb.this.dns_name
}

output "alb_zone_id" {
  description = "Zone ID del ALB, para crear un registro alias en Route 53."
  value       = aws_lb.this.zone_id
}

output "target_group_arn" {
  description = "ARN del target group donde se registran las tareas ECS."
  value       = aws_lb_target_group.this.arn
}

output "target_group_arn_suffix" {
  description = "Sufijo del ARN. Lo necesita la metrica de auto scaling por peticiones."
  value       = aws_lb_target_group.this.arn_suffix
}

output "alb_arn_suffix" {
  description = "Sufijo del ARN del ALB, para las metricas de CloudWatch."
  value       = aws_lb.this.arn_suffix
}

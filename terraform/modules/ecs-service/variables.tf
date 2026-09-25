variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Subredes privadas donde corren las tareas."
}

variable "security_group_id" {
  type        = string
  description = "Security group de las tareas."
}

variable "ecr_repository_url" {
  type        = string
  description = "URL del repositorio ECR."
}

variable "image_tag" {
  type        = string
  description = <<-EOT
    Tag de la imagen a desplegar. Usar una version inmutable (v1.2.3 o el
    SHA del commit), nunca "latest": con latest el despliegue deja de ser
    reproducible y no se puede revertir a una version concreta.
  EOT
  default     = "latest"
}

variable "container_name" {
  type        = string
  description = "Nombre del contenedor. Debe coincidir con el del target group."
  default     = "app"
}

variable "container_port" {
  type        = number
  description = "Puerto de la aplicacion."
  default     = 8080
}

variable "cpu_architecture" {
  type        = string
  description = "X86_64 o ARM64. ARM64 (Graviton) cuesta ~20% menos."
  default     = "X86_64"

  validation {
    condition     = contains(["X86_64", "ARM64"], var.cpu_architecture)
    error_message = "Debe ser X86_64 o ARM64."
  }
}

variable "task_cpu" {
  type        = number
  description = "Unidades de CPU: 256, 512, 1024, 2048 o 4096."
  default     = 512
}

variable "task_memory" {
  type        = number
  description = "Memoria en MiB. Debe ser compatible con el valor de CPU."
  default     = 1024
}

variable "desired_count" {
  type        = number
  description = "Tareas iniciales. Despues lo gobierna el auto scaling."
  default     = 2
}

variable "min_capacity" {
  type        = number
  description = "Minimo de tareas. Con 2 se sobrevive a la caida de una AZ."
  default     = 2
}

variable "max_capacity" {
  type        = number
  description = "Maximo de tareas. Es el tope de gasto ante un pico o un ataque."
  default     = 10
}

variable "cpu_target_value" {
  type        = number
  description = "Porcentaje de CPU objetivo."
  default     = 70
}

variable "memory_target_value" {
  type        = number
  description = "Porcentaje de memoria objetivo."
  default     = 75
}

variable "requests_per_target" {
  type        = number
  description = "Peticiones por tarea objetivo. null desactiva esta politica."
  default     = null
}

variable "scale_out_cooldown" {
  type        = number
  description = "Segundos antes de volver a escalar hacia arriba."
  default     = 60
}

variable "scale_in_cooldown" {
  type        = number
  description = "Segundos antes de reducir. Mas alto que scale_out para evitar flapping."
  default     = 300
}

variable "target_group_arn" {
  type        = string
  description = "Target group donde se registran las tareas."
}

variable "target_group_arn_suffix" {
  type        = string
  description = "Sufijo del target group, para la metrica de peticiones."
  default     = null
}

variable "alb_arn_suffix" {
  type        = string
  description = "Sufijo del ALB, para la metrica de peticiones."
  default     = null
}

variable "execution_role_arn" {
  type        = string
  description = "Rol del agente ECS."
}

variable "task_role_arn" {
  type        = string
  description = "Rol de la aplicacion en runtime."
}

variable "autoscaling_role_arn" {
  type        = string
  description = "Rol de Application Auto Scaling."
}

variable "environment_variables" {
  type        = map(string)
  description = "Variables NO sensibles. Quedan visibles en la consola de ECS."
  default     = {}
}

variable "container_secrets" {
  type = list(object({
    name      = string
    valueFrom = string
  }))
  description = "Secretos resueltos por ECS desde Secrets Manager en el arranque."
  default     = []
}

variable "health_check_path" {
  type        = string
  description = "Ruta del health check."
  default     = "/actuator/health"
}

variable "health_check_start_period" {
  type        = number
  description = "Segundos de gracia antes del primer chequeo. La JVM tarda en arrancar."
  default     = 90
}

variable "health_check_grace_period" {
  type        = number
  description = "Segundos antes de que el ALB empiece a evaluar una tarea nueva."
  default     = 120
}

variable "stop_timeout" {
  type        = number
  description = "Segundos para el apagado ordenado antes del SIGKILL."
  default     = 30
}

variable "deployment_maximum_percent" {
  type        = number
  description = "Porcentaje maximo de tareas durante un deploy. 200 = rolling sin caida."
  default     = 200
}

variable "deployment_minimum_healthy_percent" {
  type        = number
  description = "Porcentaje minimo de tareas sanas durante un deploy."
  default     = 100
}

variable "capacity_providers" {
  type        = list(string)
  description = "Proveedores de capacidad habilitados en el cluster."
  default     = ["FARGATE", "FARGATE_SPOT"]
}

variable "capacity_provider_strategy" {
  type = list(object({
    capacity_provider = string
    weight            = number
    base              = number
  }))
  description = "Reparto entre FARGATE y FARGATE_SPOT. Lista vacia = solo FARGATE."
  default     = []
}

variable "log_group_name" {
  type        = string
  description = "Log group de CloudWatch, creado por el modulo de composicion."
}

variable "enable_container_insights" {
  type        = bool
  description = "Metricas detalladas del cluster. Tiene costo adicional."
  default     = false
}

variable "enable_execute_command" {
  type        = bool
  description = "Permite ECS Exec. Desactivar en prod."
  default     = false
}

variable "project_name" {
  type        = string
  description = "Nombre del proyecto, prefijo de todos los recursos."
}

variable "environment" {
  type        = string
  description = "Entorno a desplegar."

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Solo se permiten dev, staging o prod."
  }
}

variable "aws_region" {
  type        = string
  description = "Region AWS."
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR de la VPC."
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  type        = list(string)
  description = "Zonas de disponibilidad. Minimo dos."
}

variable "single_nat_gateway" {
  type        = bool
  description = "true = un NAT compartido (barato). false = uno por AZ (HA)."
  default     = false
}

variable "certificate_arn" {
  type        = string
  description = "Certificado ACM para HTTPS. null deja el servicio en HTTP."
  default     = null
}

variable "container_port" {
  type        = number
  description = "Puerto de la aplicacion."
  default     = 8080
}

variable "image_tag" {
  type        = string
  description = "Tag de la imagen a desplegar. Nunca 'latest' en staging ni prod."
  default     = "latest"
}

variable "cpu_architecture" {
  type        = string
  description = "X86_64 o ARM64."
  default     = "X86_64"
}

variable "task_cpu" {
  type        = number
  description = "Unidades de CPU por tarea."
  default     = 512
}

variable "task_memory" {
  type        = number
  description = "Memoria en MiB por tarea."
  default     = 1024
}

variable "java_tool_options" {
  type        = string
  description = "Flags de la JVM. MaxRAMPercentage evita que la JVM ignore el limite del contenedor."
  default     = "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
}

variable "desired_count" {
  type        = number
  description = "Tareas iniciales."
  default     = 2
}

variable "min_capacity" {
  type        = number
  description = "Minimo de tareas."
  default     = 2
}

variable "max_capacity" {
  type        = number
  description = "Maximo de tareas. Tope de gasto."
  default     = 10
}

variable "cpu_target_value" {
  type        = number
  description = "CPU objetivo en porcentaje."
  default     = 70
}

variable "memory_target_value" {
  type        = number
  description = "Memoria objetivo en porcentaje."
  default     = 75
}

variable "requests_per_target" {
  type        = number
  description = "Peticiones por tarea objetivo. null desactiva la politica."
  default     = null
}

variable "secret_keys" {
  type        = list(string)
  description = "Claves del secreto de la aplicacion. Los valores se cargan fuera de Terraform."
  default     = []
}

variable "extra_environment_variables" {
  type        = map(string)
  description = "Variables NO sensibles adicionales."
  default     = {}
}

variable "health_check_path" {
  type        = string
  description = "Ruta del health check."
  default     = "/actuator/health"
}

variable "dynamodb_billing_mode" {
  type        = string
  description = "PAY_PER_REQUEST o PROVISIONED."
  default     = "PAY_PER_REQUEST"
}

variable "dynamodb_point_in_time_recovery" {
  type        = bool
  description = "Backup continuo. Activar en prod."
  default     = false
}

variable "dynamodb_stream_enabled" {
  type        = bool
  description = "Habilita Streams para la proyeccion CQRS."
  default     = false
}

variable "max_image_count" {
  type        = number
  description = "Imagenes etiquetadas a conservar."
  default     = 10
}

variable "secret_recovery_window_in_days" {
  type        = number
  description = "Ventana de recuperacion del secreto. 0 permite recrear rapido en dev."
  default     = 30
}

variable "log_retention_days" {
  type        = number
  description = "Retencion de logs en CloudWatch."
  default     = 30
}

variable "capacity_providers" {
  type        = list(string)
  description = "Proveedores de capacidad del cluster."
  default     = ["FARGATE", "FARGATE_SPOT"]
}

variable "capacity_provider_strategy" {
  type = list(object({
    capacity_provider = string
    weight            = number
    base              = number
  }))
  description = "Reparto FARGATE / FARGATE_SPOT."
  default     = []
}

variable "enable_container_insights" {
  type        = bool
  description = "Metricas detalladas del cluster."
  default     = false
}

variable "enable_execute_command" {
  type        = bool
  description = "ECS Exec. Solo entornos no productivos."
  default     = false
}

variable "enable_deletion_protection" {
  type        = bool
  description = "Protege el ALB contra borrado accidental."
  default     = false
}

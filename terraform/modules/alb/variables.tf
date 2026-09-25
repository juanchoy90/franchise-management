variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "vpc_id" {
  type        = string
  description = "VPC donde se crea el target group."
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Subredes publicas donde se expone el ALB."
}

variable "security_group_id" {
  type        = string
  description = "Security group del ALB."
}

variable "container_port" {
  type        = number
  description = "Puerto de la aplicacion."
  default     = 8080
}

variable "health_check_path" {
  type        = string
  description = "Ruta del health check. Con Spring Boot Actuator, /actuator/health."
  default     = "/actuator/health"
}

variable "health_check_interval" {
  type        = number
  description = "Segundos entre chequeos."
  default     = 30
}

variable "health_check_timeout" {
  type        = number
  description = "Segundos antes de considerar fallido un chequeo."
  default     = 5
}

variable "deregistration_delay" {
  type        = number
  description = "Segundos de espera antes de sacar un target. Debe superar la peticion mas lenta."
  default     = 30
}

variable "idle_timeout" {
  type        = number
  description = "Segundos que el ALB mantiene una conexion inactiva."
  default     = 60
}

variable "enable_deletion_protection" {
  type        = bool
  description = "Impide borrar el ALB por accidente. Activar en prod."
  default     = false
}

variable "certificate_arn" {
  type        = string
  description = "ARN del certificado ACM. null deja el servicio solo en HTTP."
  default     = null
}

variable "ssl_policy" {
  type        = string
  description = "Politica TLS del listener HTTPS."
  default     = "ELBSecurityPolicy-TLS13-1-2-2021-06"
}

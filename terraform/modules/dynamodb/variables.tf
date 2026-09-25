variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "billing_mode" {
  type        = string
  description = "PAY_PER_REQUEST (on-demand) o PROVISIONED."
  default     = "PAY_PER_REQUEST"

  validation {
    condition     = contains(["PAY_PER_REQUEST", "PROVISIONED"], var.billing_mode)
    error_message = "Debe ser PAY_PER_REQUEST o PROVISIONED."
  }
}

variable "point_in_time_recovery" {
  type        = bool
  description = "Backup continuo con restauracion a cualquier segundo de los ultimos 35 dias."
  default     = false
}

variable "stream_enabled" {
  type        = bool
  description = "Habilita DynamoDB Streams para proyecciones CQRS."
  default     = false
}

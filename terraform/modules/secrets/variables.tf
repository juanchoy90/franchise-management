variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "secret_keys" {
  type        = list(string)
  description = <<-EOT
    Claves JSON que contendra el secreto. Solo los NOMBRES; los valores se
    cargan fuera de Terraform. Cada clave se referencia desde la definicion
    de tarea como <arn>:<clave>::
  EOT
  default     = []
}

variable "recovery_window_in_days" {
  type        = number
  description = "Dias antes del borrado definitivo. 0 = borrado inmediato (util en dev)."
  default     = 30

  validation {
    condition     = var.recovery_window_in_days == 0 || (var.recovery_window_in_days >= 7 && var.recovery_window_in_days <= 30)
    error_message = "Debe ser 0 o estar entre 7 y 30."
  }
}

variable "rotation_lambda_arn" {
  type        = string
  description = "ARN de la Lambda de rotacion. null desactiva la rotacion automatica."
  default     = null
}

variable "rotation_days" {
  type        = number
  description = "Cada cuantos dias rotar el secreto."
  default     = 90
}

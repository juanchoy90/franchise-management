variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "max_image_count" {
  type        = number
  description = "Cuantas imagenes etiquetadas conservar antes de expirar las viejas."
  default     = 10
}

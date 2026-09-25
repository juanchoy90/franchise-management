variable "project_name" {
  description = "Nombre del proyecto. Prefija el bucket y la tabla de bloqueo."
  type        = string
  default     = "franchise"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,30}$", var.project_name))
    error_message = "Solo minusculas, numeros y guiones; entre 3 y 31 caracteres."
  }
}

variable "aws_region" {
  description = "Region donde vive el backend remoto."
  type        = string
  default     = "us-east-1"
}

variable "github_repository" {
  description = "Repositorio en formato owner/repo, para el rol OIDC de GitHub Actions."
  type        = string
  default     = "juanchoy90/franchise-management"
}

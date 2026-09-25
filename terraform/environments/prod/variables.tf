variable "project_name" {
  type    = string
  default = "franchise"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}

variable "vpc_cidr" {
  type    = string
  default = "10.30.0.0/16"
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "image_tag" {
  type        = string
  description = "Tag de la imagen. El pipeline lo inyecta con -var."
  default     = null
}

variable "secret_keys" {
  type    = list(string)
  default = []
}

variable "repository_url" {
  type        = string
  description = "URL del repositorio, para trazabilidad en las etiquetas."
  default     = "https://github.com/tu-org/franchise-api"
}

variable "certificate_arn" {
  type        = string
  description = "Certificado ACM. En produccion el trafico va por HTTPS."
  default     = null
}

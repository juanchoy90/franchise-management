variable "project_name" {
  description = "Nombre del proyecto, usado como prefijo de todos los recursos."
  type        = string
}

variable "environment" {
  description = "Entorno: dev, staging o prod."
  type        = string
}

variable "aws_region" {
  description = "Region AWS. Necesaria para el nombre del VPC endpoint."
  type        = string
}

variable "vpc_cidr" {
  description = "Rango CIDR de la VPC. Debe ser /16 para que quepan las subredes."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrsubnet(var.vpc_cidr, 8, 0))
    error_message = "El CIDR debe permitir subredes /24."
  }
}

variable "availability_zones" {
  description = "AZs donde desplegar. Minimo dos para alta disponibilidad."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) >= 2
    error_message = "Se requieren al menos 2 zonas de disponibilidad."
  }
}

variable "single_nat_gateway" {
  description = "true = un solo NAT (barato, para dev). false = uno por AZ (HA, para prod)."
  type        = bool
  default     = false
}

variable "container_port" {
  description = "Puerto en el que escucha la aplicacion dentro del contenedor."
  type        = number
  default     = 8080
}

variable "certificate_arn" {
  description = "ARN del certificado ACM. Si es null, no se abre el puerto 443."
  type        = string
  default     = null
}

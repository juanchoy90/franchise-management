variable "project_name" {
  type        = string
  description = "Nombre del proyecto."
}

variable "environment" {
  type        = string
  description = "Entorno: dev, staging o prod."
}

variable "ecr_repository_arn" {
  type        = string
  description = "ARN del repositorio ECR del que se baja la imagen."
}

variable "secret_arns" {
  type        = list(string)
  description = "ARNs exactos de los secretos que puede leer este entorno."
}

variable "kms_key_arn" {
  type        = string
  description = "Clave KMS de los secretos. null si se usa la clave gestionada por AWS."
  default     = null
}

variable "log_group_arn" {
  type        = string
  description = "ARN del log group de CloudWatch."
}

variable "dynamodb_table_arn" {
  type        = string
  description = "ARN de la tabla DynamoDB."
}

variable "dynamodb_index_arns" {
  type        = list(string)
  description = "ARNs de los indices. IAM los exige aparte del ARN de la tabla."
  default     = []
}

variable "enable_execute_command" {
  type        = bool
  description = "Permite ECS Exec. Solo para entornos no productivos."
  default     = false
}

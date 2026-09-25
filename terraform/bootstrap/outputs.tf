output "state_bucket" {
  description = "Bucket S3 del estado remoto. Va en el backend.tf de cada entorno."
  value       = aws_s3_bucket.state.id
}

output "lock_table" {
  description = "Tabla DynamoDB de bloqueo. Va en el backend.tf de cada entorno."
  value       = aws_dynamodb_table.lock.name
}

output "backend_config_snippet" {
  description = "Bloque listo para pegar en environments/<env>/backend.tf"
  value       = <<-EOT
    terraform {
      backend "s3" {
        bucket         = "${aws_s3_bucket.state.id}"
        key            = "<env>/terraform.tfstate"
        region         = "${var.aws_region}"
        dynamodb_table = "${aws_dynamodb_table.lock.name}"
        encrypt        = true
      }
    }
  EOT
}

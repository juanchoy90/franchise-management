terraform {
  backend "s3" {
    bucket         = "franchise-tfstate-CAMBIAR_POR_ACCOUNT_ID"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "franchise-tflock"
    encrypt        = true
  }
}

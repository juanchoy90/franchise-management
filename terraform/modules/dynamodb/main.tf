resource "aws_dynamodb_table" "this" {
  name         = "${var.project_name}-${var.environment}"
  billing_mode = var.billing_mode
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  attribute {
    name = "GSI1PK"
    type = "S"
  }

  attribute {
    name = "stock"
    type = "N"
  }

  global_secondary_index {
    name            = "GSI1"
    hash_key        = "GSI1PK"
    range_key       = "stock"
    projection_type = "INCLUDE"

    non_key_attributes = ["name", "id", "branchId", "franchiseId"]
  }

  point_in_time_recovery {
    enabled = var.point_in_time_recovery
  }

  server_side_encryption {
    enabled = true
  }

  stream_enabled   = var.stream_enabled
  stream_view_type = var.stream_enabled ? "NEW_AND_OLD_IMAGES" : null

  tags = { Name = "${var.project_name}-${var.environment}" }
}

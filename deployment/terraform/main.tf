terraform {
  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.19.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.1.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "3.1.0"
    }
  }
}

data "azuread_client_config" "current" {}

# TODO create AzureAD identity in Terraform
variable "az-service-account-name" {
  type = string
  default = "id-lyp-refimp-dev"
}
resource "azuread_application" "application" {
  for_each = var.applications
  display_name = "lyp-${var.namespace}-${each.key}"

  owners           = [data.azuread_client_config.current.object_id]
  # single tenant
  sign_in_audience = "AzureADMyOrg"

  api {
    requested_access_token_version = 2
  }

  required_resource_access {
    resource_app_id = "00000003-0000-0000-c000-000000000000" # Microsoft Graph

    resource_access {
      id   = "e1fe6dd8-ba31-4d61-89e7-88639da4683d" # User.Read
      type = "Scope"
    }
  }
}

resource "azuread_application_federated_identity_credential" "resource_server_federated_creds" {
  for_each = azuread_application.application
  application_object_id = azuread_application.application.object_id
  display_name          = "${azuread_application.application.display_name}-creds"
  description           = "Federeated Credentials for ${azuread_application.application.display_name}"
  audiences             = ["api://AzureADTokenExchange"]
  issuer                = var.aks-internal-id-url
  subject               = "system:serviceaccount:${var.namespace}:${az-service-account-name}"
}

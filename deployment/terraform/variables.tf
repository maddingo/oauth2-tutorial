variable "namespace" {
  type = string
  default = "reference-implementation"
}

variable "applications" {
  type = map(any)
  default = {
    "resource-server" = {
      "name" = "resource-server"
      "roller" = {
        "message.read" = {
          "allowed-member-types" = ["Application"],
        },
        "message.write" = {
          "allowed-member-types" = ["Application", "User"],
        },
      }
    }
  }
}

variable "aks-internal-id-url" {
  type = string
  default = "https://westeurope.oic.prod-aks.azure.com/22ca942f-06c2-4f38-9407-0e447dedbb67/877d0e6a-ac05-4dd6-a0bc-9fd9753b66c8/"
}
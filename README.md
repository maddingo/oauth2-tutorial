[![Build status](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/build.yml/badge.svg)](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/build.yml)
# OAuth2 with OIDC playground
This project runs 3 servers:
- Authorization Server ([Documentation](authorization-server/README.md))
- Resource Server
- Client Application

The code originates from https://github.com/spring-projects/spring-authorization-server/tree/main/samples with some slight modifications.

# Authorization Code Flow
![Authorization Code Flow](https://plantuml-dev.snartibox.net/proxy?cache=no&src=https://raw.githubusercontent.com/Lyse-AS/lyp-reference-implementation/develop/doc/pkce.puml)

# Client Credential Flow
![Client Secret Flow](https://plantuml-dev.snartibox.net/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Lyse-AS/lyp-reference-implementation/develop/doc/client-credentials.puml)


# Development
Running the Resource Server and the Client Application requires a running Authorization Server.

Run the Script `start-apps.sh` to start all 3 servers.

## Build without Azure Container Registry
```shell
docker run -d -p 5000:5000 --restart=always --name registry registry:2
mvn clean install -Dacr.publish=false -Dartifacts.server=localhost:5000
```
This will tag the images with `localhost:5000` as docker registry.

## Spring Boot and Rest
See: https://developer.okta.com/blog/2022/06/17/simple-crud-react-and-spring-boot


### Run the application on Azure
1. Create a service account in Azure AD `id-lyp-refimp-id`
2. 
2. 
```bash
cd aks
../connect-to-cluser.sh
az identity create --name "id-lyp-refimp-dev" --resource-group "rg-lyp-weu-aks-dev" --subscription "988ae0be-8cdc-4730-b118-8b765c4b6d0c"

{
  "clientId": "aa203dd7-41ac-454c-92d1-d6daf4b009a0",
  "id": "/subscriptions/988ae0be-8cdc-4730-b118-8b765c4b6d0c/resourcegroups/rg-lyp-weu-aks-dev/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-lyp-refimp-dev",
  "location": "westeurope",
  "name": "id-lyp-refimp-dev",
  "principalId": "c870c02d-2ca9-4bdf-a7cc-f67897f4cda9",
  "resourceGroup": "rg-lyp-weu-aks-dev",
  "systemData": null,
  "tags": {},
  "tenantId": "22ca942f-06c2-4f38-9407-0e447dedbb67",
  "type": "Microsoft.ManagedIdentity/userAssignedIdentities"
}


kubectl apply -f client-app.yaml

```

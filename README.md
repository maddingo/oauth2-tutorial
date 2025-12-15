[![Build status](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/build.yml/badge.svg)](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/build.yml)
# OAuth2 with OIDC playground
This project runs 3 servers:
- Authorization Server ([Documentation](authorization-server/README.md))
- Resource Server
- Client Application

TODO : get inspired by https://github.com/eugenp/tutorials.git spring-security-models/spring-security-pkce and spring-security-models/spring-security-pkce-spa

https://docs.spring.io/spring-authorization-server/reference/guides/how-to-userinfo.html

The code originates from https://github.com/spring-projects/spring-authorization-server/tree/main/samples with some slight modifications.

# Authorization Code Flow
```mermaid
sequenceDiagram
    autonumber
    actor user
    participant client-app
    participant authorization-server
    participant resource-server

    rect rgb(135, 150, 90)
    Note right of user: Getting the Access Token
    user ->> client-app: Click login link
    client-app ->> client-app: create code_verifier
    client-app ->> authorization-server: redirect to /authorize endpoint
    authorization-server ->> user: redirect to user login
    user ->> authorization-server: login and consent
    authorization-server ->> client-app: send authorization code to callback URL
    client-app ->> client-app: authorization code + code verifier to /token endpoint
    authorization-server ->> authorization-server: validate code verifier and challenge
    authorization-server ->> client-app: send id token + access token
    end
    rect rgb(55, 155, 0)
    Note right of client-app: Requesting Data
    client-app ->> resource-server: request data with access token
    resource-server ->> client-app: response with data
    end
```

# Client Credential Flow
```mermaid
sequenceDiagram
    autonumber
    participant client-app
    participant authorization-server
    participant resource-server

    rect rgb(135, 150, 90)
    Note right of client-app: Getting the Access Token
    client-app ->> authorization-server: authenticate with client-id + client-secret to /token endpoint
    authorization-server ->> authorization-server: validate client-id + client-secret
    authorization-server ->> client-app: send access token
    end
    rect rgb(55, 155, 0)
    Note right of client-app: Requesting Data
    client-app ->> resource-server: request data with access token
    resource-server ->> client-app: response with data
    end
```


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
See https://github.com/Lyse-AS/deploy-my-application-to-k8s

### Git Workflow
```mermaid
gitGraph
%%    commit
    commit id: "  "
    commit id: " "
    branch develop
    branch feature/ABC-123-develop-new-feature
    checkout feature/ABC-123-develop-new-feature
    commit
    commit
    checkout develop
    branch feature/ABC-124-develop-new-feature
    commit
    commit
    checkout feature/ABC-123-develop-new-feature
    commit
    checkout develop
    merge  feature/ABC-123-develop-new-feature
    checkout feature/ABC-124-develop-new-feature
    commit
    checkout develop
    merge feature/ABC-124-develop-new-feature
    checkout main
    merge develop
    commit id: "Release" tag: "v1.0.0"
%%    commit
```

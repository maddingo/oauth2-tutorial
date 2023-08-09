[![Build status](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/maven.yml/badge.svg)](https://github.com/Lyse-AS/oauth2-playground/actions/workflows/maven.yml)
# OAuth2 with OIDC playground
This project runs 3 servers:
- Authorization Server
- Resource Server
- Client Application

The code originates from https://github.com/spring-projects/spring-authorization-server/tree/main/samples with some slight modifications.

# Authorization Code Flow
![Authorization Code Flow](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/maddingo/oauth2-playground/develop/doc/pkce.puml)

# Client Credential Flow
![Client Secret Flow](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/maddingo/oauth2-playground/develop/doc/client-credentials.puml)


# Development
Running the Resource Server and the Client Application requires a running Authorization Server.

Run the Script `start-apps.sh` to start all 3 servers.

## Deploy to local Maven Repository
```shell
mvn clean deploy -DaltDeploymentRepository=local::file://${HOME}/tmp/maven-local -Dspring-boot.build-image.skip=true -Dmaven.deploy.skip=false
```
## Start Docker Registry
```shell
docker run -d -p 5000:5000 --restart=always --name registry registry:2
``` 

## Deploy to local Docker registry
```shell
mvn clean deploy -Dspring-boot.build-image.publish=true -Ddocker.image.registry=localhost:5000
```

## Spring Boot and Rest
See: https://developer.okta.com/blog/2022/06/17/simple-crud-react-and-spring-boot

## Client Application
The client app has a Spring boot backend and a Next.js frontend

### Develop the backend

```bash
mvn clean compile spring-boot:run
```

### Develop the frontend

```bash
mvn frontend:install-node-and-npm frontend:npm@npm-run-dev -Pdev
```

or simply

```bash
cd frontend; npm run dev
```

### Run the packaged application

```bash
mvn clean package ; java -jar target/client-app.jar
```

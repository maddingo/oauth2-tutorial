# Authorization server 
__The authorization server can be used for testing purposes. It should never be used in production.__

## Running in Docker
Add the `application.yml` to the config folder:
```yaml

server:
  port: 9000

#debug: true

auth-server:
  issuer: 'http://idp:8080'
  users:
    - username: "user1"
      password: "{noop}password"
      roles:
        - "USER"

spring:
  security:
    oauth2:
      authorizationserver:
        client:
          messaging-client:
            registration:
              client-id: "messaging-client"
              client-secret: "{noop}secret"
              client-authentication-methods:
                - "client_secret_basic"
              authorization-grant-types:
                - "authorization_code"
                - "refresh_token"
                - "client_credentials"
              redirect-uris:
                - "http://client-app:8080/login/oauth2/code/messaging-client-oidc"
                - "http://client-app:8080/login/oauth2/code/messaging-client-authorization-code"
                - "http://client-app:8080/authorized"
#              post-logout-redirect-uris:
#                - "http://127.0.0.1:8080/"
              scopes:
                - "openid"
                - "profile"
                - "message.read"
                - "message.write"
            require-authorization-consent: true

logging:
  level:
    root: info
  #   org.springframework.security: debug
```

Run the docker container:
```shell
docker run -p 9000:9000 -v $(pwd)/config:/workspace/config acrlypfelles.azurecr.io/authorization-server:2.0.0-SNAPSHOT
```

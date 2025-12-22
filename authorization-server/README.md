# Authorization Server 
__The authorization server can be used for testing purposes. It should NOT be used in production.__

## Running in Docker
The Authorization Code flow requires a back channel to the client application. I.e. the authorization-server must be able to talk to the 
client application. 

The reason for setting up an alias for the idp is that the browser will send the wrong cookie to the idp, if the idp and the client-app share the same host name.

In order to do this, you need to add a host entry for the client application and the idp to your `/etc/hosts` file:
```text
127.0.0.1 idp client-app
```

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

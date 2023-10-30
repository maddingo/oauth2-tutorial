package no.lyse.plattform.oauth2playground.client.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
public class AuthorizationController {

    // '/authorized' is the registered 'redirect_uri' for authorization_code
    @GetMapping(value = "/authorized")
    public Mono<ResponseEntity<OAuth2Error>> authorizationFailed(
        @RequestParam(name = OAuth2ParameterNames.ERROR, required = false) String errorCode,
        @RequestParam(name = OAuth2ParameterNames.ERROR_DESCRIPTION, required = false) String errorDescription,
        @RequestParam(name = OAuth2ParameterNames.ERROR_URI, required = false) String errorUri
    ) {
        OAuth2Error error = new OAuth2Error(
            errorCode,
            errorDescription,
            errorUri);
        log.warn(error.toString());

        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    @GetMapping(value = "/authorize")
    public Mono<ResponseEntity<Void>> authorizationCodeGrant(ServerWebExchange exchange) {

        return Mono.just(ResponseEntity.ok().build());
    }

//    @GetMapping(value = "/authorize", params = "grant_type=authorization_code")
//    public Mono<ResponseEntity<Void>> authorizationCodeGrant(
//                                         @RegisteredOAuth2AuthorizedClient("messaging-client-authorization-code")
//                                         OAuth2AuthorizedClient authorizedClient) {
//
//        return Mono.just(ResponseEntity.ok().build());
//    }
//
//    @GetMapping(value = "/authorize", params = "grant_type=client_credentials")
//    public Mono<ResponseEntity<Void>> clientCredentialsGrant() {
//
//        return Mono.just(ResponseEntity.ok().build());
//    }
}

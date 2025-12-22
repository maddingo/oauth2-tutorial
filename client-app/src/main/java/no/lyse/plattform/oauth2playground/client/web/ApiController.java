package no.lyse.plattform.oauth2playground.client.web;

import lombok.extern.slf4j.Slf4j;
import no.lyse.plattform.oauth2playground.quotesapi.model.Quote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    private final WebClient webClient;
    private final String messagesBaseUri;

    private final OAuth2ClientProperties clientProperties;

    public ApiController(
        WebClient webClient,
        @Value("${messages.base-uri}") String messagesBaseUri,
        OAuth2ClientProperties clientProperties) {
        this.webClient = webClient;
        this.messagesBaseUri = messagesBaseUri;
        this.clientProperties = clientProperties;
    }

    @GetMapping("/quote")
    public Mono<Quote> quote() {
        return getQuote(clientRegistrationId("messaging-client-client-credentials"));
    }

    @GetMapping("/quote1")
    public Mono<Quote> quote1(
        //@RegisteredOAuth2AuthorizedClient("messaging-client-authorization-code")
        @RegisteredOAuth2AuthorizedClient("messaging-client-oidc")
        OAuth2AuthorizedClient authorizedClient
    ) {
        return getQuote(oauth2AuthorizedClient(authorizedClient));
    }

    @GetMapping("/auth/clients")
    public ResponseEntity<Mono<Collection<String>>> clients() {
        return ResponseEntity.ok(Mono.just(clientProperties.getRegistration().keySet()));
    }

    @GetMapping(path = "/auth/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> session(ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .switchIfEmpty(Mono.just(() -> "anonymous"))
            .map(Principal::getName)
            .zipWith(exchange.getSession())
            .map(t -> {
                if (t.getT1().equals("anonymous")) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(
                    Map.of(
                        "user",
                        Map.of(
                            "id", "1",
                            "name", t.getT1(),
                            "email", t.getT1() + "@test.no",
                            "image", "/pingu_hahn.jpg"
                        ),
                        "expires", formatSessionExpiration(t.getT2())
                    )
                );
            });
    }

    /*
    @GetMapping(path = "/auth/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> session(ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .doOnCancel(() -> log.info("Principal cancelled"))
            .log(ApiController.class.getName(), Level.FINE, SignalType.ON_NEXT, SignalType.ON_ERROR)
            .map(Principal::getName)
            .zipWith(exchange.getSession())
//            .log(ApiController.class.getName(), Level.FINE, SignalType.ON_NEXT, SignalType.ON_ERROR)
            .map(t -> Map.of(
                "user",
                Map.of(
                    "id", "1",
                    "name", t.getT1(),
                    "email", t.getT1() + "@test.no",
                    "image", "/pingu_hahn.jpg"
                ),
                "expires", formatSessionExpiration(t.getT2())))
            .map(c -> {
                if (c == null) {
                    return ResponseEntity.noContent().<Map<String, Object>>build();
                } else {
                    return ResponseEntity.ok(c);
                }
            })
            .doOnCancel(() -> log.info("Session cancelled"))
            .onErrorResume(e ->
                Mono.defer(() -> {
                    log.error("Returning empty session", e);
                    return Mono.just(ResponseEntity.noContent().<Map<String, Object>>build());
                })
            );
    }*/

    /**
     * Required by React-auth.
     */
    @PostMapping(path = "/auth/_log")
    public Mono<Map<String, String>> log(ServerWebExchange exchange) {
        return stringContent(exchange)
            .map(content -> {
                log.info(content);
                return Map.of("status", "ok");
            });
    }

    private static Mono<String> stringContent(ServerWebExchange exchange) {
        if (isUrlFormEncoded(exchange)) {
            return exchange.getFormData()
                .map(Map::toString);
        } else {
            return exchange.getRequest().getBody()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .collect(Collectors.joining());
        }
    }

    /**
     * Is the media type of the exchange compatible with the given mediaType.
     * This function uses save navigation of properties.
     */
    private static boolean isUrlFormEncoded(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange)
            .map(ServerWebExchange::getRequest)
            .map(HttpMessage::getHeaders)
            .map(HttpHeaders::getContentType)
            .map(c -> c.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .orElse(false);
    }

    private static String formatSessionExpiration(WebSession session) {
        ZonedDateTime expiryTime = session.getLastAccessTime().plus(session.getMaxIdleTime()).atOffset(ZoneOffset.UTC).toZonedDateTime();
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiryTime);
    }

    private Mono<Quote> getQuote(Consumer<Map<String, Object>> clientAttribute) {
        return this.webClient
            .get()
            .uri(UriComponentsBuilder.fromUriString(messagesBaseUri).path("/quote").build().toUri())
            .attributes(clientAttribute)
            .retrieve()
            .bodyToMono(Quote.class)
            .log();
    }

}

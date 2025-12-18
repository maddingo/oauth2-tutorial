package no.lyse.plattform.oauth2playground.resourceserver.web;

import lombok.RequiredArgsConstructor;
import no.lyse.plattform.oauth2playground.resourceserver.api.Api;
import no.lyse.plattform.oauth2playground.resourceserver.data.QuotesRepository;
import no.lyse.plattform.oauth2playground.resourceserver.model.Quote;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ResourceController implements Api {

    private final QuotesRepository quotes;

    @Override
    public Mono<ResponseEntity<Mono<Quote>>> getQuote(String id, ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .map(Principal::getName)
            .log()
            .flatMap(name ->
                quotes.getQuote(id)
                    .map(Mono::just)
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build())
            );
    }

    @Override
    public Mono<ResponseEntity<Mono<Quote>>> getRandomQuote(ServerWebExchange exchange) {
        return quotes.randomQuote()
            .map(Mono::just)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Flux<Quote> getQuotes(ServerWebExchange exchange) {
        return quotes.quotes();
    }
}

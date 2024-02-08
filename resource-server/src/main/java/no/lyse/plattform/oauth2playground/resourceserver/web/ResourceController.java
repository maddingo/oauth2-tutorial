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

@RestController
@RequiredArgsConstructor
public class ResourceController implements Api {

    private final QuotesRepository quotes;

    @Override
    public Mono<Quote> getQuote(String id, ServerWebExchange exchange) {
        return quotes.getQuote(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Element with id '%s' not found", id))));
        // TODO: if we can generate Mono<ResponseEntity<Mono<Quote>>> instead of ResponseEntity<Mono<Quote>> we can use this code:
//        return quotes.getQuote(id)
//            .map(q -> ResponseEntity.ok(Mono.just(q)));
    }

    @Override
    public Mono<Quote> getRandomQuote(ServerWebExchange exchange) {
        return quotes.randomQuote();
        // TODO: if we can generate Mono<ResponseEntity<Mono<Quote>>> instead of ResponseEntity<Mono<Quote>> we can use this code:
//        return quotes.randomQuote()
//            .map(q -> ResponseEntity.ok(Mono.just(q)));
    }

    @Override
    public Flux<Quote> getQuotes(ServerWebExchange exchange) {
        return quotes.quotes();
        // TODO: if we can generate Mono<ResponseEntity<Mono<Quote>>> instead of ResponseEntity<Mono<Quote>> we can use this code:
//        return Mono.just(ResponseEntity.ok(quotes.quotes()));
    }
}

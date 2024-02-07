package no.lyse.plattform.oauth2playground.resourceserver.web;

import lombok.RequiredArgsConstructor;
import no.lyse.plattform.oauth2playground.resourceserver.api.Api;
import no.lyse.plattform.oauth2playground.resourceserver.data.QuotesRepository;
import no.lyse.plattform.oauth2playground.resourceserver.model.Quote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ResourceController implements Api {

    private final QuotesRepository quotes;

    @Override
    public Mono<ResponseEntity<Mono<Quote>>> getQuote(String id, ServerWebExchange exchange) {
        return quotes.getQuote(id)
            .map(q -> ResponseEntity.ok(Mono.just(q)));
    }

    @Override
    public Mono<ResponseEntity<Mono<Quote>>> getRandomQuote(ServerWebExchange exchange) {
        return quotes.randomQuote()
            .map(q -> ResponseEntity.ok(Mono.just(q)));
    }

    @Override
    public Mono<ResponseEntity<Flux<Quote>>> getQuotes(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(quotes.quotes()));
    }
}

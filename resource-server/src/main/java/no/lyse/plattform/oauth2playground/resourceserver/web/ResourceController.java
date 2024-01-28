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

    /**
     * Very bad implementation of a quote service. This walks through the entire list of quotes, and filters out the one.
     */
    @Override
    public ResponseEntity<Mono<Quote>> getQuote(String id, ServerWebExchange exchange) {
        return ResponseEntity.ok(quotes.getQuote(id));
    }

    @Override
    public ResponseEntity<Mono<Quote>> getRandomQuote(ServerWebExchange exchange) {
        return ResponseEntity.ok(quotes.randomQuote());
    }

    @Override
    public ResponseEntity<Flux<Quote>> getQuotes(ServerWebExchange exchange) {
        return ResponseEntity.ok(quotes.quotes());
    }
}

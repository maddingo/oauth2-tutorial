package no.lyse.plattform.oauth2playground.resourceserver.web;

import lombok.RequiredArgsConstructor;
import no.lyse.plattform.oauth2playground.quotesapi.api.QuoteApi;
import no.lyse.plattform.oauth2playground.quotesapi.api.QuotesApi;
import no.lyse.plattform.oauth2playground.quotesapi.model.Quote;
import no.lyse.plattform.oauth2playground.resourceserver.data.QuotesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ResourceController implements QuotesApi, QuoteApi {

    private final QuotesRepository quotes;

    /**
     * Very bad implementation of a quote service. This walks through the entire list of quotes, and filters out the one.
     */
    @Override
    public Mono<ResponseEntity<Quote>> getQuote(String id, ServerWebExchange exchange) {
        return quotes.getQuote(id)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Override
    public Mono<ResponseEntity<Quote>> getRandomQuote(ServerWebExchange exchange) {
        return quotes.randomQuote()
            .log()
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<Quote>>> getQuotes(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(quotes.quotes()));
    }
}

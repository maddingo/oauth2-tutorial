package no.lyse.plattform.oauth2playground.resourceserver.data;

import no.lyse.plattform.oauth2playground.quotesapi.model.Quote;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class QuotesRepository {

    private final Map<String, Quote> quoteMap = new HashMap<>();

    private final AtomicInteger lastIndex = new AtomicInteger(0);
    public Mono<Quote> randomQuote() {
        List<Quote> quotes = new ArrayList<>(quoteMap.values());
        return Mono.fromSupplier(() -> lastIndex.updateAndGet(u ->
            {
                if (quotes.isEmpty()) {
                   return -1;
                } else if (u >= (quotes.size() - 1)) {
                    return 0;
                } else {
                    return u + 1;
                }
            }))
            .filter(idx -> idx >= 0)
            .log()
            .map(quotes::get);
    }

    public Flux<Quote> quotes() {
        return Flux.fromIterable(quoteMap.values());
    }
    public void addQuote(Quote quote) {
        quoteMap.put(quote.getId(), quote);
    }

    public Mono<Quote> getQuote(String id) {
        return Mono.justOrEmpty(quoteMap.get(id));
    }
}

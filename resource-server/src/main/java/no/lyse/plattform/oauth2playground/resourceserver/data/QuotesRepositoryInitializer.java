package no.lyse.plattform.oauth2playground.resourceserver.data;

import lombok.RequiredArgsConstructor;
import no.lyse.plattform.oauth2playground.quotesapi.model.Quote;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuotesRepositoryInitializer implements CommandLineRunner {

    private final QuotesRepository quotesRepository;

    @Override
    public void run(String... args) {
        int count = 0;

        quotesRepository.addQuote(
            new Quote()
                .id(String.valueOf(++count))
                .author("Arthur Schramm")
                .quote("Im Wald da steht ein Ofenrohr, stell dir mal die Hitze vor.")
        );
        quotesRepository.addQuote(
            new Quote()
                .id(String.valueOf(++count))
                .author("Arthur Schramm")
                .quote("Die Schwalbe ist ein lustig Tier und fliegt auch um die Kirchturmspitz'. Der Löwe ist kein lustig Tier und fliegt nicht um die Kirchturmspitz'.")
        );
        quotesRepository.addQuote(
            new Quote()
                .id(String.valueOf(++count))
                .author("Arthur Schramm")
                .quote("Rumpeldibumpel, weg war der Kumpel, Schippe d'rauf, Glück Auf!.")
        );
//        quotesRepository.addQuote(
//            new Quote()
//                .id(String.valueOf(++count))
//                .author("Thomas Mann")
//                .quote("No man remains quite what he was when he recognizes himself.")
//        );
//        quotesRepository.addQuote(
//            new Quote()
//                .id(String.valueOf(++count))
//                .author("Hermann Hesse")
//                .quote("If you hate a person, you hate something in him that is part of yourself. What isn't part of ourselves doesn't disturb us.")
//        );
//        quotesRepository.addQuote(
//            new Quote()
//                .id(String.valueOf(++count))
//                .author("Lech Walesa")
//                .quote("I'm lazy. But it's the lazy people who invented the wheel and the bicycle because they didn't like walking or carrying things.")
//        );
//        quotesRepository.addQuote(
//            new Quote()
//                .id(String.valueOf(++count))
//                .author("Alan Kay")
//                .quote("The best way to predict the future is to invent it.")
//        );
    }
}

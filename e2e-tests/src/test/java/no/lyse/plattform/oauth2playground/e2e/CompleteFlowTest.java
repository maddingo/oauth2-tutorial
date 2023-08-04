package no.lyse.plattform.oauth2playground.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CompleteFlowTest {

    @Test
    void loginAndGetJokes() throws Exception {
        try(
            AppContainer idp = AppContainer.idp();
            AppContainer resourceServer = AppContainer.resourceServer();
            AppContainer clientApp = AppContainer.clientApp();
        ) {
            // IdP has to finish before we can start the other applications
            CompletableFuture<ContainerState> idpFuture = idp.call();
            ContainerState idpState = idpFuture.get(60L, java.util.concurrent.TimeUnit.SECONDS);

            List<ContainerState> apps = Stream.of(resourceServer.call(), clientApp.call())
                .map(CompletableFuture::join)
                .toList();

            // Now we can start testing
            try (
                Playwright pw = Playwright.create();
                Browser browser = pw.chromium().launch()
            ) {

                Page page = browser.newPage();
                page.navigate("http://localhost:8080/");
//                page.screenShot(new Page.ScreenshotOptions().setPath(Paths.get("target", "screenshot.png")));
                
            }
        }
    }
}

package no.lyse.plattform.oauth2playground.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CompleteFlowTest {

    boolean headless = Boolean.getBoolean("e2etest.headless");

    @Test
    void loginAndGetJokes() throws Exception {

        try(
            AppContainer idp = AppContainer.idp();
            AppContainer resourceServer = AppContainer.resourceServer();
            AppContainer clientApp = AppContainer.clientApp();
        ) {
            // IdP has to finish before we can start the other applications
            CompletableFuture<AppContainer.State> idpFuture = idp.call();
            AppContainer.State idpState = idpFuture.get(60L, java.util.concurrent.TimeUnit.SECONDS);

            List<AppContainer.State> apps = Stream.of(resourceServer.call(), clientApp.call())
                .map(CompletableFuture::join)
                .toList();

            // Now we can start testing
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
            try (
                Playwright pw = Playwright.create();
                Browser browser = pw.chromium().launch(launchOptions)
            ) {
                Page page = browser.newPage();
                page.navigate("http://localhost:8080/");

                // wait for the components to load
                page.waitForLoadState(LoadState.NETWORKIDLE);
                // there should be one heading from the quotes component
                List<Locator> arthurSchrammJokes = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(2)).all();
                MatcherAssert.assertThat(arthurSchrammJokes, org.hamcrest.Matchers.hasSize(1));
                arthurSchrammJokes.forEach(joke -> {
                    assertThat(joke).isVisible();
                    MatcherAssert.assertThat(joke.innerText(), org.hamcrest.Matchers.containsString("Arthur Schramm"));
                });

                page.click("text=Sign In");
                assertThat(page).hasURL("http://auth-server:9000/login");
                page.fill("input[name=\"username\"]", "user1");
                page.fill("input[name=\"password\"]", "password");
                page.click("button[type=\"submit\"]");
                assertThat(page).hasURL(Pattern.compile("http://auth-server:9000/oauth2/authorize\\?.*"));
                page.check("input[id=\"profile\"]");
                page.check("input[id=\"message.read\"]");
                page.check("input[id=\"message.write\"]");
                page.click("button[type=\"submit\"]");

                // wait for the components to load
                page.waitForLoadState(LoadState.NETWORKIDLE);
                // there should be two headings from the quotes component
                arthurSchrammJokes = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(2)).all();
                MatcherAssert.assertThat(arthurSchrammJokes, org.hamcrest.Matchers.hasSize(2));
                arthurSchrammJokes.forEach(joke -> {
                    assertThat(joke).isVisible();
                    MatcherAssert.assertThat(joke.innerText(), org.hamcrest.Matchers.containsString("Arthur Schramm"));
                });
                
            }
        }
    }
}

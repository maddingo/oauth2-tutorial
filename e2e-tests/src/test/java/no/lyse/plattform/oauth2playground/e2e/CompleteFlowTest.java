package no.lyse.plattform.oauth2playground.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CompleteFlowTest {

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
            BrowserType.LaunchOptions launchOptions = null; //new BrowserType.LaunchOptions().setHeadless(false);
            try (
                Playwright pw = Playwright.create();
                Browser browser = pw.chromium().launch(launchOptions)
            ) {
                Page page = browser.newPage();
                page.navigate("http://localhost:8080/");
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
//                page.screenShot(new Page.ScreenshotOptions().setPath(Paths.get("target", "screenshot.png")));
                
            }
        }
    }
}

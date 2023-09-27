package no.lyse.plattform.oauth2playground.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebDriverBuilder;
import org.openqa.selenium.remote.http.ClientConfig;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
public class CompleteFlowTest {

    boolean headless = Boolean.parseBoolean(System.getProperty("e2etest.headless", "true"));

    @Test
    void loginAndGetQuotes() throws Exception {

        try(
            AppContainer idp = AppContainer.idp();
            AppContainer resourceServer = AppContainer.resourceServer();
            AppContainer clientApp = AppContainer.clientApp();
        ) {
            // IdP has to finish before we can start the other applications
            CompletableFuture<AppContainer.State> idpFuture = idp.call();
            AppContainer.State idpState = idpFuture.get(60L, java.util.concurrent.TimeUnit.SECONDS);

            List<AppContainer.State> appsState = Stream.of(resourceServer.call(), clientApp.call())
                .map(CompletableFuture::join)
                .toList();

            AppContainer.State resourceServerState = appsState.get(0);
            AppContainer.State clientAppState = appsState.get(1);
            URI clientAppUri = new URI("http", null, clientAppState.hostname(), clientAppState.mappedPort(), "/", null, null);
            URI idpUri = new URI("http", null, idpState.hostname(), idpState.mappedPort(), "/", null, null);

            // Now we can start testing
            runPlaywrightTest(clientAppUri, idpUri);
        }
    }

    @Test
    void loginWithDocker() {
        String releaseVersion = System.getProperty("release.version", "2.0.0-SNAPSHOT");
        String containerRegistry = System.getProperty("artifacts.server", "acrlypfelles.azurecr.io");

        try (AllApps apps = new AllApps(containerRegistry, releaseVersion)) {
            apps.start();
            URI clientAppUri = apps.getClientAppUri();
            URI idpUri = URI.create("http://idp:8080");
            runSeleniumTest(clientAppUri, idpUri, apps.getNetwork());
        }
    }

    @SneakyThrows
    private void runSeleniumTest(URI clientAppUri, URI idpUri, Network network) {
        Path recordingPath = Path.of("target", "recordings").toAbsolutePath();
        Files.createDirectories(recordingPath);
        try (
            BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
                .withCapabilities(new ChromeOptions())
                .withNetwork(network)
                .withNetworkAliases("browser")
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("BRW"))
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, recordingPath.toFile())
        ) {
            chrome.start();
            WebDriver driver = RemoteWebDriver.builder()
                .oneOf(new ChromeOptions())
                .address(chrome.getSeleniumAddress())
                .config(ClientConfig.defaultConfig())
                .build();

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30L));

            driver.get("http://client-app:8080");
            MatcherAssert.assertThat(driver.findElements(By.tagName("h2")), allOf(
                hasSize(1),
                everyItem(
                    hasProperty("text", startsWith("Arthur Schramm"))
                )
            ));

            driver.findElement(By.linkText("Sign In")).click();
            MatcherAssert.assertThat(driver.getCurrentUrl(), startsWith("http://idp:8080/login"));
            driver.findElement(By.name("username")).sendKeys("user1");
            driver.findElement(By.name("password")).sendKeys("password");
            driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();
            MatcherAssert.assertThat(driver.getCurrentUrl(), startsWith("http://idp:8080/oauth2/authorize"));
            check(driver.findElement(By.id("profile")));
            check(driver.findElement(By.id("message.read")));
            check(driver.findElement(By.id("message.write")));
            driver.findElement(By.cssSelector("button[type=\"submit\"]")).click();

            MatcherAssert.assertThat(driver.getCurrentUrl(), startsWith("http://client-app:8080"));

            // we wait for the refresh button, because the panel is dynamically loaded
            driver.findElement(By.xpath("//button[text()=\"Refresh\"]")).isDisplayed();
            MatcherAssert.assertThat(driver.findElements(By.tagName("h2")), allOf(
                hasSize(2),
                everyItem(hasProperty("text", startsWith("Arthur Schramm")))
            ));
        }
    }

    private void check(WebElement element) {
        if (!element.isSelected()) {
            element.click();
        }
    }

    private void runPlaywrightTest(URI clientAppUri, URI idpUri) {

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
        try (
            Playwright pw = Playwright.create();
            Browser browser = pw.chromium().launch(launchOptions)
        ) {
            Page page = browser.newPage();
            page.navigate(clientAppUri.toString());

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
            assertThat(page).hasURL(idpUri.resolve("/login").toString());
            page.fill("input[name=\"username\"]", "user1");
            page.fill("input[name=\"password\"]", "password");
            page.click("button[type=\"submit\"]");
            assertThat(page).hasURL(Pattern.compile(idpUri.resolve("/oauth2/authorize") + "\\?.*"));
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

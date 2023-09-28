package no.lyse.plattform.oauth2playground.e2e;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.hamcrest.Matchers.*;

@Slf4j
public class CompleteFlowTest {

    @Test
    void loginAndGetQuotes() {
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

}

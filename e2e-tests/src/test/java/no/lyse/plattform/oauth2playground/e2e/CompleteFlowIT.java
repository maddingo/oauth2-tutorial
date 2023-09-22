package no.lyse.plattform.oauth2playground.e2e;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import io.netty.util.internal.SocketUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Slf4j
public class CompleteFlowIT {

    @Test
    @SneakyThrows
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

            runTest(clientAppUri, idpUri, null, null, true, false);
        }
    }

    @Test
    @SneakyThrows
    void loginAndGetQuotesDockerCompose() {
        try (DockerComposeContainer<?> container = new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("auth-server", 8080)
            .withExposedService("resource-server", 8080)
            .withExposedService("client-app", 8080)
            .withLocalCompose(true)
            .withPull(false)
            .withTailChildContainers(true)
            .withTailChildContainers(true)
            .withLogConsumer("idp", new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("idp")))
            .withLogConsumer("resource-server", new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("resource-server")))
            .withLogConsumer("client-app", new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("client-app")))
            .waitingFor("client-app", Wait.forHealthcheck()) ) {

            container.start();

            URI clientAppUri = new URI("http", null, "localhost", container.getServicePort("client-app", 8080), "/", null, null);
            URI idpUri = new URI("http", null, "localhost", container.getServicePort("auth-server", 8080), "/", null, null);

            runTest(clientAppUri, idpUri, null, null, false, true);
        }
    }

    @Test
    @SneakyThrows
    void loginAndGetQuotesDocker() {
        String releaseVersion = System.getProperty("release.version", "2.0.0-SNAPSHOT");
        String containerRegistry = System.getProperty("artifacts.server", "acrlypfelles.azurecr.io");
        int port = org.springframework.test.util.TestSocketUtils.findAvailableTcpPort();


//        URI idpUri = new URI("http", null, "localhost", port, "/", null, null);
        URI idpUri = URI.create("http://idp:8080");
        Path recordingPath = Path.of("target", "recording").toAbsolutePath();
        Files.createDirectories(recordingPath);
        try (
            Network network = Network.newNetwork();
            GenericContainer<?> idp =
                new GenericContainer<>(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("authorization-server").withTag(releaseVersion))
                    .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
                    .withNetwork(network)
                    .withNetworkAliases("idp")
                    .withExposedPorts(8080)
//                     .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(port), new ExposedPort(8080)))))
                    .withCommand(
                        "--auth-server.issuer=" + idpUri,
                        "--server.port=8080",
                        "--redirect.server-uris=http://browser:8080/,http://browser:8080/login/oauth2/code/messaging-client-oidc,http://browser:8080/login/oauth2/code/messaging-client-authorization-code,http://browser:8080/authorized"
                    )
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("IDP"));
            GenericContainer<?> resourceServer =
                new GenericContainer<>(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("resource-server").withTag(releaseVersion))
                    .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
                    .withNetwork(network)
                    .withNetworkAliases("resource-server")
                    .withExposedPorts(8080)
                    .dependsOn(idp)
                    .withCommand(
                        "--spring.security.oauth2.resourceserver.jwt.issuer-uri=" + idpUri,
                        "--server.port=8080"
                    )
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("RES"));
            GenericContainer<?> clientApp =
                new GenericContainer<>(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("client-app").withTag(releaseVersion))
                    .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
                    .withNetwork(network)
                    .withNetworkAliases("client-app")
                    .withExposedPorts(8080)
                    .dependsOn(idp)
                    .withCommand(
                        "--spring.security.oauth2.client.provider.spring.issuer-uri=" + idpUri,
                        "--server.port=8080",
                        "--messages.base-uri=http://resource-server:8080",
                        "--spring.security.oauth2.client.registration.messaging-client-oidc.redirect-uri=http://client-app:8080/login/oauth2/code/{registrationId}"
                    )
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("CLI"));

            BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
                .withEnv("JAVA_OPTS", "-Djdk.httpclient.websocket.intermediateBufferSize=3000000")
                .withCapabilities(new ChromeOptions())
                .withNetwork(network)
                .withNetworkAliases("browser")
                .dependsOn(clientApp)
                .withExposedPorts(4444)
                .withSharedMemorySize(2_000_000_000L)
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, recordingPath.toFile(), VncRecordingContainer.VncRecordingFormat.MP4)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("BRW")
                );
        ) {
            var idpHost = idp.getHost();
//            idp.getContainerInfo().getHostConfig().get
            idp.start();
            resourceServer.start();
            clientApp.start();
            chrome.start();
            URI clientAppUri = new URI("http", null, "client-app", 8080, "/", null, null);
//            URI seleniumUri = new URI("http", null, chrome.getContainerIpAddress(), chrome.getMappedPort(4444), "/", null, null);
            URI seleniumUri = chrome.getSeleniumAddress().toURI();
            runTest(clientAppUri, idpUri, seleniumUri, recordingPath, true, false);
        }
    }

    @Test
    @Tag("docker-compose")
    void loginAndGetQuotesInsideDockerCompose() {
        URI clientAppUri = URI.create("http://client-app:8080");
        URI idpUri = URI.create("http://idp:8080");
        runTest(clientAppUri, idpUri, null, null, true, true);
    }

    private void runTest(URI clientAppUri, URI idpUri, URI seleniumUri, Path recordingPath, boolean withLogin, boolean headless) {
        // Now we can start testing
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
        Playwright.CreateOptions createOptions = seleniumUri != null ? new Playwright.CreateOptions().setEnv(Map.of("SELENIUM_REMOTE_URL", seleniumUri.toString())) : null;
        try (
            Playwright pw = Playwright.create(createOptions);
            Browser browser = pw.chromium().launch(launchOptions)
        ) {
            if (recordingPath != null) {
                browser.newContext(new Browser.NewContextOptions().setRecordVideoDir(recordingPath).setRecordVideoSize(1920, 1080));
            }
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

            if (!withLogin) {
                return;
            }
            page.click("text=Sign In");
            assertThat(page).hasURL(Pattern.compile(idpUri.resolve("/login") + ".*"));
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

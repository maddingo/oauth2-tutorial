package no.lyse.plattform.oauth2playground.e2e;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class AllApps implements Startable {
    private final String containerRegistry;
    private final String imageTag;

    @Getter
    private final Set<Startable> dependencies = new HashSet<>();

    private final CompletableFuture<URI> clientAppUri = new CompletableFuture<>();

    @Getter
    private final Network network = Network.newNetwork();

    public AllApps(String containerRegistry, String imageTag) {
        this.containerRegistry = containerRegistry;
        this.imageTag = imageTag;

        GenericContainer<?> idp = createIdp();
        GenericContainer<?> resourceServer = createResourceServer(idp);
        GenericContainer<?> clientApp = createClientApp(idp, resourceServer);

        dependencies.add(idp);
        dependencies.add(resourceServer);
        dependencies.add(clientApp);
    }

    @Override
    public void start() {
        // generic container's start makes sure the dependencies are started first
        dependencies.forEach(Startable::start);
    }

    private GenericContainer<?> createClientApp(GenericContainer<?> idp, GenericContainer<?> resourceServer) {
        // The mapped port is only available after the container is started
        return new ClientAppContainer(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("no.lyse.plattform.oauth2-playground.client-app").withTag(imageTag))
            .withWhenStarted(c -> clientAppUri.complete(URI.create("http://" + c.getHost() + ":" + c.getMappedPort(8080)))) // This wait strategy allows us to declare the containers locally, the mapped port is only available after the container is started
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
            .withNetwork(network)
            .withNetworkAliases("client-app")
            .withExposedPorts(8080)
            .dependsOn(idp, resourceServer)
            .withCommand(
                "--spring.security.oauth2.client.provider.spring.issuer-uri=http://idp:8080",
                "--server.port=8080",
                "--messages.base-uri=http://resource-server:8080",
                "--spring.security.oauth2.client.registration.messaging-client-oidc.redirect-uri=http://client-app:8080/login/oauth2/code/{registrationId}"
            )
            .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("CLI"))
            ;
    }

    private GenericContainer<?> createResourceServer(GenericContainer<?> idp) {
        return new GenericContainer<>(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("no.lyse.plattform.oauth2-playground.resource-server").withTag(imageTag))
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
            .withNetwork(network)
            .withNetworkAliases("resource-server")
            .withExposedPorts(8080)
            .dependsOn(idp)
            .withCommand(
                "--spring.security.oauth2.resourceserver.jwt.issuer-uri=http://idp:8080",
                "--server.port=8080"
            )
            .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("RES"));
    }

    private GenericContainer<?> createIdp() {
        return new GenericContainer<>(DockerImageName.parse("").withRegistry(containerRegistry).withRepository("no.lyse.plattform.oauth2-playground.authorization-server").withTag(imageTag))
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).allowInsecure().forStatusCode(200))
            .withNetwork(network)
            .withNetworkAliases("idp")
            .withExposedPorts(8080)
            .withClasspathResourceMapping("/e2e-test/application.yml", "/workspace/config/application.yml", BindMode.READ_ONLY)
            .withCommand(
                "--logging.level.org.springframework.security=TRACE"
            )
            .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("IDP"));
    }

    @Override
    public void stop() {
        dependencies.forEach(Startable::stop);
        network.close();
    }

    /**
     * clientAppUri available after containers are started.
     * wait for it to be available, or timeout after 10 seconds.
     */
    @SneakyThrows
    public URI getClientAppUri() {
        return clientAppUri.get(10L, TimeUnit.SECONDS);
    }

    private static class ClientAppContainer extends GenericContainer<ClientAppContainer> {
        private Consumer<ClientAppContainer> whenStarted;

        public ClientAppContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }

        public GenericContainer<ClientAppContainer> withWhenStarted(Consumer<ClientAppContainer> whenStarted) {
            this.whenStarted = whenStarted;
            return this;
        }
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            if (whenStarted != null) {
                whenStarted.accept(this);
            }
        }
    }
}

package no.lyse.plattform.oauth2playground.e2e;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppContainer implements AutoCloseable, Callable<CompletableFuture<ContainerState>> {
    private Process process;
    private final String appName;
    private final String jarFile;
    private final String hostname;
    private final int port;

    public void close() throws Exception {
        if (process != null) {
            process.destroy();
        }
    }

    public static AppContainer idp() {
        return new AppContainer("IdP", "../authorization-server/target/authorization-server.jar", "auth-server", 9000);
    }

    public static AppContainer resourceServer() {
        return new AppContainer("Resource-Server", "../resource-server/target/resource-server.jar", "localhost", 8090);
    }

    public static AppContainer clientApp() {
        return new AppContainer("Client-App", "../client-app/target/client-app.jar", "localhost", 8080);
    }

    public CompletableFuture<ContainerState> call() {
        return CompletableFuture.supplyAsync(this::startJar);
    }

    protected ContainerState startJar() {
        try {
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(java, "-jar", jarFile);
            File outFile = File.createTempFile("e2e-" + appName, ".log");
            pb.redirectOutput(outFile);
            log.info("Starting {}", appName);
            process = pb.start();
            try (BufferedReader reader = new BufferedReader(new FileReader(outFile))) {
                // If we try to read the stream too soon, we don't get any output
                // This also guards against the process already being terminated
                process.waitFor(3L, TimeUnit.SECONDS);
                log.info("Hopefully started {}", appName);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("changed to ACCEPTING_TRAFFIC")) {
                        return new ContainerState(hostname, port);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("Could not start " + appName);
    }
}

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
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppContainer implements AutoCloseable, Callable<CompletableFuture<AppContainer.State>> {
    private Process process;
    private final String appName;
    private final String jarFile;
    private final String hostname;
    private final int port;
    private final Function<String, Boolean> waitCondition;

    public void close() throws Exception {
        if (process != null) {
            process.destroy();
        }
    }

    public static AppContainer idp() {
        return new AppContainer(
            "IdP",
            "../authorization-server/target/authorization-server.jar",
            "auth-server",
            9000,
            line -> line.contains("changed to ACCEPTING_TRAFFIC")
        );
    }

    public static AppContainer resourceServer() {
        return new AppContainer(
            "Resource-Server",
            "../resource-server/target/resource-server.jar",
            "localhost",
            8090,
            line -> line.contains("changed to ACCEPTING_TRAFFIC")
        );
    }

    public static AppContainer clientApp() {
        return new AppContainer(
            "Client-App",
            "../client-app/target/client-app.jar",
            "localhost",
            8080,
            line -> line.contains("Started ClientApplication")
        );
    }

    public CompletableFuture<State> call() {
        return CompletableFuture.supplyAsync(this::startJar);
    }

    protected State startJar() {
        try {
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(java, "-jar", jarFile);
            File outFile = File.createTempFile("e2e-" + appName, ".log");
            pb.redirectOutput(outFile);
            log.info("Starting {}", appName);
            process = pb.start();
            try (BufferedReader reader = new BufferedReader(new FileReader(outFile))) {
                // If we try to read the stream too soon, we don't get any output
                log.info("Checking if {} is ready", appName);
                String line;
                int tryCount = 0;
                while ((line = reader.readLine()) != null || tryCount < 10) {
                    if (line != null) {
                        if (waitCondition.apply(line)) {
                            log.info("Started {}", appName);
                            return new State(hostname, port);
                        }
                    } else {
                        log.info("Waiting for {} to start, attempt {}", appName, tryCount);
                        Thread.sleep(1000L + tryCount * 1000L);
                        tryCount++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("Could not start " + appName);
    }

    public record State(
        String ipAddress, Integer mappedPort
    ) {
    }
}

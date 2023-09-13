package no.lyse.plattform.oauth2playground.e2e;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class AppContainer implements AutoCloseable, Callable<CompletableFuture<AppContainer.State>> {
    public static final String JAVA_BIN = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    private Process process;
    private final ProcessBuilder processBuilder;
    private final String appName;
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
            new ProcessBuilder(JAVA_BIN, "-jar", "../authorization-server/target/authorization-server.jar", "--auth-server.issuer=http://localhost:9000"),
            "IdP",
            "localhost",
            9000,
            line -> line.contains("changed to ACCEPTING_TRAFFIC")
        );
    }

    public static AppContainer resourceServer() {
        return new AppContainer(
            new ProcessBuilder(JAVA_BIN, "-jar", "../resource-server/target/resource-server.jar", "--spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000"),
            "Resource-Server",
            "localhost",
            8090,
            line -> line.contains("changed to ACCEPTING_TRAFFIC")
        );
    }

    public static AppContainer clientApp() {
        return new AppContainer(
            new ProcessBuilder(JAVA_BIN, "-jar", "../client-app/target/client-app.jar", "--spring.security.oauth2.client.provider.spring.issuer-uri=http://localhost:9000"),
            "Client-App",
            "localhost",
            8080,
            line -> line.contains("Started ClientApplication")
        );
    }

    public CompletableFuture<State> call() {
        return CompletableFuture.supplyAsync(this::startJar);
    }

    protected State startJar() {
        File outFile;
        File errFile;
        try {
            outFile = File.createTempFile("e2e-" + appName, "-out.log");
            outFile.deleteOnExit();
            errFile = File.createTempFile("e2e-" + appName, "-err.log");
            errFile.deleteOnExit();
            processBuilder.redirectOutput(outFile);
            processBuilder.redirectError(errFile);
            log.info("Starting {}", appName);
            process = processBuilder.start();
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
                    if (!process.isAlive()) {
                        throw new IllegalStateException("Process died: " + Files.readString(errFile.toPath()));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (outFile.exists()) {
            try {
                log.error("Could not start {}:\n{}", appName, Files.readString(outFile.toPath()));
            } catch (Exception e) {
                log.error("Could not start {}", appName, e);
            }
        }
        throw new IllegalStateException("Could not start " + appName);
    }

    public record State(
        String hostname, Integer mappedPort
    ) {
    }
}

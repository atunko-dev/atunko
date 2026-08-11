package io.github.atunkodev.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class DaemonClientTest {

    private static final String VERSION = "0.1.0-TEST";
    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";

    @TempDir
    Path registryDir;

    @TempDir
    Path projectRoot;

    private DaemonRegistry registry;
    private DaemonServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(projectRoot.resolve("A.java"), "import java.util.List;\nclass A {}\n");
        registry = new DaemonRegistry(registryDir, entry -> {});
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    private DaemonEntry startServer(String version) throws Exception {
        server = new DaemonServer(projectRoot, version, Duration.ofMinutes(5), new RecipeExecutionEngine(), registry);
        server.register();
        serverThread = new Thread(server::serve, "daemon-client-test");
        serverThread.setDaemon(true);
        serverThread.start();
        return server.entry();
    }

    /** A launcher that never actually starts a JVM — these tests drive the client, not process spawning. */
    private DaemonLauncher refusingLauncher() {
        return new DaemonLauncher(registry, Duration.ofMillis(1)) {
            @Override
            public Optional<DaemonEntry> launch(Path root, String version) {
                return Optional.empty();
            }
        };
    }

    private DaemonMessage.Execute request() {
        return new DaemonMessage.Execute(List.of(RECIPE), Map.of(), false);
    }

    @Test
    @Timeout(120)
    void usesARunningDaemonWithoutStartingOne() throws Exception {
        startServer(VERSION);
        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        DaemonClient.Attempt attempt = client.execute(projectRoot, request());

        assertThat(attempt.result()).isPresent();
        assertThat(attempt.startedDaemon()).isFalse();
        assertThat(attempt.fallbackReason()).isNull();
    }

    @Test
    @Timeout(120)
    @SVCs({"atunko:SVC_CORE_0023.4"})
    void replacesDaemonOnVersionMismatch() throws Exception {
        startServer("0.0.1-OLD");
        assertThat(registry.find(projectRoot)).isPresent();

        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        assertThat(client.usableEntry(projectRoot))
                .as("a daemon of another version must not be reused")
                .isEmpty();
        assertThat(registry.find(projectRoot))
                .as("the mismatched daemon is stopped and deregistered")
                .isEmpty();
    }

    @Test
    @Timeout(60)
    @SVCs({"atunko:SVC_CLI_0009.1"})
    void reportsFallbackWhenNoDaemonCanBeStarted() {
        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        DaemonClient.Attempt attempt = client.execute(projectRoot, request());

        assertThat(attempt.result()).isEmpty();
        assertThat(attempt.fallbackReason()).contains("did not start");
    }

    @Test
    @Timeout(60)
    @SVCs({"atunko:SVC_CLI_0009.1"})
    void reportsFallbackAndClearsEntryWhenDaemonIsUnreachable() {
        // An entry pointing at a port nothing is listening on — what a crashed daemon leaves behind.
        registry.write(new DaemonEntry(
                projectRoot, unusedPort(), ProcessHandle.current().pid(), VERSION, "token", 1L));
        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        DaemonClient.Attempt attempt = client.execute(projectRoot, request());

        assertThat(attempt.result()).isEmpty();
        assertThat(attempt.fallbackReason()).contains("unreachable");
        assertThat(registry.find(projectRoot))
                .as("a dead entry is cleared so the next run starts fresh")
                .isEmpty();
    }

    @Test
    @Timeout(120)
    void statusQueriesARunningDaemon() throws Exception {
        DaemonEntry entry = startServer(VERSION);
        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        Optional<DaemonMessage.StatusResult> status = client.status(entry);

        assertThat(status).isPresent();
        assertThat(status.get().atunkoVersion()).isEqualTo(VERSION);
    }

    @Test
    @Timeout(120)
    void stopShutsDownAndDeregisters() throws Exception {
        DaemonEntry entry = startServer(VERSION);
        DaemonClient client = new DaemonClient(registry, refusingLauncher(), VERSION);

        client.stop(entry);

        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    void disabledByConfigurationFollowsTheSystemProperty() {
        assertThat(DaemonClient.disabledByConfiguration()).isFalse();
        System.setProperty(DaemonClient.DISABLED_PROPERTY, "true");
        try {
            assertThat(DaemonClient.disabledByConfiguration()).isTrue();
        } finally {
            System.clearProperty(DaemonClient.DISABLED_PROPERTY);
        }
    }

    private static int unusedPort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

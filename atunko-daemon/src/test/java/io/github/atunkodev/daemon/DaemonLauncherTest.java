package io.github.atunkodev.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the real thing: a separate JVM, started by the launcher, serving a request over a socket. The rest of
 * the daemon tests run the server in-thread, so this is the only place that proves the spawn actually works.
 */
class DaemonLauncherTest {

    private static final String VERSION = "0.1.0-TEST";

    @TempDir
    Path registryDir;

    @TempDir
    Path projectRoot;

    private DaemonRegistry registry;
    private DaemonClient client;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(projectRoot.resolve("A.java"), "import java.util.List;\nclass A {}\n");
        // The child JVM resolves its own registry from this property, so both sides must agree on the directory.
        System.setProperty(DaemonDirs.REGISTRY_DIR_PROPERTY, registryDir.toString());
        System.setProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY, "20");
        registry = new DaemonRegistry(registryDir);
        client = new DaemonClient(registry, VERSION);
    }

    @AfterEach
    void tearDown() {
        registry.list().forEach(entry -> ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy));
        System.clearProperty(DaemonDirs.REGISTRY_DIR_PROPERTY);
        System.clearProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY);
    }

    @Test
    @Timeout(300)
    @SVCs({"atunko:SVC_CORE_0023"})
    void startsARealDaemonProcessThatServesTwoRequestsWithOneParse() throws Exception {
        DaemonClient.Attempt first =
                client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));

        assertThat(first.fallbackReason())
                .as("daemon should have served the request")
                .isNull();
        assertThat(first.startedDaemon()).isTrue();
        assertThat(first.result().orElseThrow().parsedFromCache()).isFalse();

        DaemonEntry entry = registry.find(projectRoot).orElseThrow();
        assertThat(entry.pid())
                .as("the daemon must be a separate process")
                .isNotEqualTo(ProcessHandle.current().pid());

        DaemonClient.Attempt second =
                client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));

        assertThat(second.startedDaemon()).as("the running daemon is reused").isFalse();
        assertThat(second.result().orElseThrow().parsedFromCache())
                .as("the second run across process boundaries must not re-parse — this is the whole feature")
                .isTrue();
    }

    @Test
    @Timeout(300)
    void stopTerminatesTheSpawnedProcess() throws Exception {
        client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));
        DaemonEntry entry = registry.find(projectRoot).orElseThrow();
        assertThat(entry.isProcessAlive()).isTrue();

        client.stop(entry);

        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
        while (entry.isProcessAlive() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(entry.isProcessAlive()).isFalse();
        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    @Timeout(300)
    void launcherWritesADaemonLogFile() throws Exception {
        Optional<DaemonEntry> entry = new DaemonLauncher(registry).launch(projectRoot, VERSION);

        assertThat(entry).isPresent();
        assertThat(DaemonLauncher.logFile(DaemonRegistry.resolve(projectRoot))).exists();
    }

    private static String recipe() {
        return "org.openrewrite.java.RemoveUnusedImports";
    }
}

package io.github.atunkodev.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.atunkodev.testing.DaemonDiagnostics;
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
        // Long enough that the daemon cannot expire mid-test. A short timeout here made this flaky on CI: the
        // first request legitimately takes minutes (the daemon builds its recipe environment on first use), and a
        // daemon that idles out between the request returning and the registry lookup takes its entry with it.
        // Idle expiry has its own dedicated test — DaemonServerTest.exitsAfterIdleTimeout — which runs in-process
        // with a 500ms timeout and does not depend on how fast the machine is.
        System.setProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY, "600");
        // Bound the spawned JVMs: unbounded, each daemon claims a quarter of the host's memory, and a CI runner
        // running two of them alongside the build has killed one mid-request.
        System.setProperty(DaemonLauncher.MAX_HEAP_PROPERTY, "1g");
        registry = new DaemonRegistry(registryDir);
        client = new DaemonClient(registry, VERSION);
    }

    @AfterEach
    void tearDown() {
        registry.list().forEach(entry -> ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy));
        System.clearProperty(DaemonDirs.REGISTRY_DIR_PROPERTY);
        System.clearProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY);
        System.clearProperty(DaemonLauncher.MAX_HEAP_PROPERTY);
    }

    @Test
    @Timeout(300)
    @SVCs({"atunko:SVC_CORE_0023"})
    void startsARealDaemonProcessThatServesTwoRequestsWithOneParse() throws Exception {
        DaemonClient.Attempt first =
                client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));

        assertThat(first.fallbackReason())
                .withFailMessage(() -> diagnostics(first.fallbackReason()))
                .isNull();
        assertThat(first.startedDaemon()).isTrue();
        assertThat(first.result().orElseThrow().parsedFromCache()).isFalse();

        DaemonEntry entry = registeredDaemon();
        assertThat(entry.pid())
                .as("the daemon must be a separate process")
                .isNotEqualTo(ProcessHandle.current().pid());

        DaemonClient.Attempt second =
                client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));

        assertThat(second.fallbackReason())
                .withFailMessage(() -> diagnostics(second.fallbackReason()))
                .isNull();
        assertThat(second.startedDaemon()).as("the running daemon is reused").isFalse();
        assertThat(second.result().orElseThrow().parsedFromCache())
                .as("the second run across process boundaries must not re-parse — this is the whole feature")
                .isTrue();
    }

    @Test
    @Timeout(300)
    void stopTerminatesTheSpawnedProcess() throws Exception {
        DaemonClient.Attempt attempt =
                client.execute(projectRoot, new DaemonMessage.Execute(List.of(recipe()), Map.of(), false));

        // Checked before the lookup below: a fallback takes the registry entry with it, and the resulting empty
        // Optional reports nothing about why the daemon was unusable.
        assertThat(attempt.fallbackReason())
                .withFailMessage(() -> diagnostics(attempt.fallbackReason()))
                .isNull();
        DaemonEntry entry = registeredDaemon();
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

        assertThat(entry)
                .withFailMessage(() -> diagnostics("the launcher returned no entry"))
                .isPresent();
        assertThat(DaemonLauncher.logFile(DaemonRegistry.resolve(projectRoot))).exists();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0023.5"})
    void configuredMaxHeapBoundsTheDaemonJvm() {
        System.setProperty(DaemonLauncher.MAX_HEAP_PROPERTY, "1g");

        List<String> command = new DaemonLauncher(registry).command(DaemonRegistry.resolve(projectRoot), VERSION);

        assertThat(command).contains("-Xmx1g");
        assertThat(command)
                .as("the child starts daemons of its own, so it needs the setting too")
                .contains("-D" + DaemonLauncher.MAX_HEAP_PROPERTY + "=1g");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0023.5"})
    void withoutAConfiguredMaxHeapTheDaemonKeepsJvmDefaults() {
        // Undoes the bound setUp applies for the spawn tests — this one is about the unconfigured default.
        System.clearProperty(DaemonLauncher.MAX_HEAP_PROPERTY);

        List<String> command = new DaemonLauncher(registry).command(DaemonRegistry.resolve(projectRoot), VERSION);

        assertThat(command).noneMatch(arg -> arg.startsWith("-Xmx"));
    }

    /**
     * The daemon the test just started. Asserted rather than {@code orElseThrow}-ed: a bare
     * {@code NoSuchElementException} here is what made issue #91 impossible to diagnose from a CI log.
     */
    private DaemonEntry registeredDaemon() {
        Optional<DaemonEntry> entry = registry.find(projectRoot);
        assertThat(entry)
                .withFailMessage(() -> diagnostics("the daemon served the request but has no registry entry"))
                .isPresent();
        return entry.orElseThrow();
    }

    private String diagnostics(String reason) {
        return DaemonDiagnostics.describe(registryDir, reason);
    }

    private static String recipe() {
        return "org.openrewrite.java.RemoveUnusedImports";
    }
}

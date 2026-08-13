package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.daemon.AtunkoVersion;
import io.github.atunkodev.daemon.DaemonDirs;
import io.github.atunkodev.daemon.DaemonEntry;
import io.github.atunkodev.daemon.DaemonLauncher;
import io.github.atunkodev.daemon.DaemonRegistry;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.atunkodev.testing.DaemonDiagnostics;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class DaemonCommandTest {

    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";

    @TempDir
    Path registryDir;

    @TempDir
    Path projectDir;

    private DaemonRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty(DaemonDirs.REGISTRY_DIR_PROPERTY, registryDir.toString());
        // Long enough that a daemon cannot idle out mid-test — the first run builds the recipe environment and
        // takes minutes on CI. Teardown kills whatever is still running, so nothing outlives the suite.
        System.setProperty("atunko.daemon.idle-timeout", "600");
        // Bound the spawned JVMs: unbounded, each daemon claims a quarter of the host's memory, and this suite
        // starts two of them alongside the build.
        System.setProperty(DaemonLauncher.MAX_HEAP_PROPERTY, "1g");
        Files.writeString(projectDir.resolve("Sample.java"), "import java.util.List;\nclass Sample {}\n");
        registry = new DaemonRegistry(registryDir);
    }

    @AfterEach
    void tearDown() {
        // Skips this JVM: some tests register a synthetic entry carrying the current pid.
        long self = ProcessHandle.current().pid();
        registry.list().forEach(entry -> {
            if (entry.pid() != self) {
                ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy);
            }
        });
        System.clearProperty(DaemonDirs.REGISTRY_DIR_PROPERTY);
        System.clearProperty("atunko.daemon.idle-timeout");
        System.clearProperty(DaemonLauncher.MAX_HEAP_PROPERTY);
    }

    @Test
    void statusReportsNothingWhenNoDaemonRuns() {
        CommandLineFixture fixture = CommandLineFixture.create();

        int exit = fixture.execute("daemon", "status");

        assertThat(exit).isZero();
        assertThat(fixture.stdout()).contains("No atunko daemons running.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0009.3"})
    void statusListsProjectRootPortPidAndIdleTime() {
        registry.write(new DaemonEntry(
                projectDir,
                45678,
                ProcessHandle.current().pid(),
                AtunkoVersion.current(),
                "token",
                System.currentTimeMillis()));

        CommandLineFixture fixture = CommandLineFixture.create();
        int exit = fixture.execute("daemon", "status");

        assertThat(exit).isZero();
        assertThat(fixture.stdout())
                .contains("PID")
                .contains("PORT")
                .contains("IDLE")
                .contains("PROJECT")
                .contains("45678")
                .contains(String.valueOf(ProcessHandle.current().pid()))
                .contains(projectDir.toString());
    }

    @Test
    @Timeout(600)
    @SVCs({"atunko:SVC_CLI_0009.3"})
    void stopTerminatesTheDaemonForAProjectAndClearsItsEntry() {
        CommandLineFixture run = CommandLineFixture.create();
        run.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());
        assertDaemonServed(run);
        assertThat(registry.list()).as("a daemon should be running to stop").hasSize(1);

        CommandLineFixture fixture = CommandLineFixture.create();
        int exit = fixture.execute("daemon", "stop", "--project-dir", projectDir.toString());

        assertThat(exit).isZero();
        assertThat(fixture.stdout()).contains("Stopped daemon");
        assertThat(registry.list()).isEmpty();
    }

    @Test
    @Timeout(600)
    @SVCs({"atunko:SVC_CLI_0009.3"})
    void stopAllTerminatesEveryDaemon() throws Exception {
        Path second = Files.createDirectory(projectDir.resolve("nested"));
        Files.writeString(second.resolve("Other.java"), "import java.util.Map;\nclass Other {}\n");

        CommandLineFixture first = CommandLineFixture.create();
        first.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());
        assertDaemonServed(first);
        CommandLineFixture nested = CommandLineFixture.create();
        nested.execute("run", "-r", RECIPE, "--project-dir", second.toString());
        assertDaemonServed(nested);
        assertThat(registry.list()).hasSize(2);

        CommandLineFixture fixture = CommandLineFixture.create();
        int exit = fixture.execute("daemon", "stop", "--all");

        assertThat(exit).isZero();
        assertThat(registry.list()).isEmpty();
    }

    /**
     * Checked before any assertion on the registry: a run that fell back to in-process execution leaves no daemon
     * behind, so the registry assertion fails with a count that says nothing about the cause. {@code run} reports
     * the fallback on stderr.
     */
    private void assertDaemonServed(CommandLineFixture run) {
        assertThat(run.stderr())
                .withFailMessage(() -> DaemonDiagnostics.describe(registryDir, run.stderr()))
                .doesNotContain("Daemon unavailable");
    }

    @Test
    void stopReportsWhenThereIsNothingToStop() {
        CommandLineFixture fixture = CommandLineFixture.create();

        int exit = fixture.execute("daemon", "stop", "--project-dir", projectDir.toString());

        assertThat(exit).isZero();
        assertThat(fixture.stdout()).contains("No atunko daemon running for");
    }

    @Test
    void daemonWithoutASubcommandShowsStatus() {
        CommandLineFixture fixture = CommandLineFixture.create();

        int exit = fixture.execute("daemon");

        assertThat(exit).isZero();
        assertThat(fixture.stdout()).contains("No atunko daemons running.");
    }
}

package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.daemon.DaemonClient;
import io.github.atunkodev.daemon.DaemonDirs;
import io.github.atunkodev.daemon.DaemonRegistry;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end behaviour of {@code atunko run} with the daemon in play, including the case that matters most: the
 * daemon must not change the answer.
 */
class RunCommandDaemonTest {

    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";

    @TempDir
    Path registryDir;

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty(DaemonDirs.REGISTRY_DIR_PROPERTY, registryDir.toString());
        // Short-lived daemons: these tests must not leave a 30-minute process behind on a developer's machine.
        System.setProperty("atunko.daemon.idle-timeout", "30");
        Files.writeString(projectDir.resolve("Sample.java"), """
            import java.util.List;
            import java.util.Map;

            class Sample {
                Map<String, String> field;
            }
            """);
    }

    @AfterEach
    void tearDown() {
        // Skips this JVM: some tests register a synthetic entry carrying the current pid.
        long self = ProcessHandle.current().pid();
        new DaemonRegistry(registryDir).list().forEach(entry -> {
            if (entry.pid() != self) {
                ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy);
            }
        });
        System.clearProperty(DaemonDirs.REGISTRY_DIR_PROPERTY);
        System.clearProperty("atunko.daemon.idle-timeout");
        System.clearProperty(DaemonClient.DISABLED_PROPERTY);
    }

    private String sourceNow() throws Exception {
        return Files.readString(projectDir.resolve("Sample.java"));
    }

    @Test
    @Timeout(600)
    @SVCs({"atunko:SVC_CLI_0009"})
    void daemonRunMatchesInProcessRun() throws Exception {
        String original = sourceNow();

        CommandLineFixture inProcess = CommandLineFixture.create();
        int inProcessExit =
                inProcess.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString(), "--no-daemon");
        String afterInProcess = sourceNow();

        // Reset and repeat through the daemon.
        Files.writeString(projectDir.resolve("Sample.java"), original);

        CommandLineFixture viaDaemon = CommandLineFixture.create();
        int daemonExit = viaDaemon.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());
        String afterDaemon = sourceNow();

        assertThat(daemonExit).as("exit status must match").isEqualTo(inProcessExit);
        assertThat(afterDaemon)
                .as("the daemon must produce byte-identical output to in-process execution")
                .isEqualTo(afterInProcess);
        assertThat(viaDaemon.stdout()).isEqualTo(inProcess.stdout());
    }

    @Test
    @Timeout(300)
    @SVCs({"atunko:SVC_CLI_0009.1"})
    void fallsBackWhenDaemonUnreachable() throws Exception {
        // A registry entry pointing at a port nothing listens on: what a crashed daemon leaves behind.
        new DaemonRegistry(registryDir)
                .write(new io.github.atunkodev.daemon.DaemonEntry(
                        projectDir.toRealPath(),
                        unusedPort(),
                        ProcessHandle.current().pid(),
                        io.github.atunkodev.daemon.AtunkoVersion.current(),
                        "token",
                        System.currentTimeMillis()));

        CommandLineFixture fixture = CommandLineFixture.create();
        int exit = fixture.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());

        assertThat(exit).as("a daemon problem must not fail the run").isZero();
        assertThat(fixture.stderr()).contains("Daemon unavailable").contains("running in this process");
        assertThat(sourceNow()).as("the recipe still applied").doesNotContain("import java.util.List;");
    }

    @Test
    @Timeout(300)
    @SVCs({"atunko:SVC_CLI_0009.2"})
    void noDaemonFlagStartsNoDaemon() {
        CommandLineFixture fixture = CommandLineFixture.create();

        int exit = fixture.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString(), "--no-daemon");

        assertThat(exit).isZero();
        assertThat(new DaemonRegistry(registryDir).list())
                .as("--no-daemon must neither start nor contact a daemon")
                .isEmpty();
        assertThat(fixture.stderr()).doesNotContain("Started an atunko daemon");
    }

    @Test
    @Timeout(300)
    @SVCs({"atunko:SVC_CLI_0009.2"})
    void disabledPropertyStartsNoDaemon() {
        System.setProperty(DaemonClient.DISABLED_PROPERTY, "true");

        CommandLineFixture fixture = CommandLineFixture.create();
        int exit = fixture.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());

        assertThat(exit).isZero();
        assertThat(new DaemonRegistry(registryDir).list()).isEmpty();
    }

    @Test
    @Timeout(600)
    void announcesTheDaemonItStarted() {
        CommandLineFixture fixture = CommandLineFixture.create();

        fixture.execute("run", "-r", RECIPE, "--project-dir", projectDir.toString());

        assertThat(fixture.stderr())
                .as("auto-start is only acceptable if the user is told about it")
                .contains("Started an atunko daemon")
                .contains("atunko daemon stop");
    }

    @Test
    @Timeout(300)
    void userRecipeSourcesBypassTheDaemon() throws Exception {
        Path recipesFile = projectDir.resolve("recipes.yml");
        Files.writeString(recipesFile, """
            type: specs.openrewrite.org/v1beta/recipe
            name: io.example.NoOp
            displayName: No-op
            description: Does nothing.
            recipeList: []
            """);

        CommandLineFixture fixture = CommandLineFixture.create();
        fixture.execute(
                "run",
                "-r",
                "io.example.NoOp",
                "--project-dir",
                projectDir.toString(),
                "--recipes-file",
                recipesFile.toString());

        assertThat(new DaemonRegistry(registryDir).list())
                .as("the daemon builds its environment from its own classpath and would not see this recipe")
                .isEmpty();
    }

    private static int unusedPort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

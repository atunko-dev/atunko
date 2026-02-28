package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunCommandTest {

    private static final Path FIXTURE_DIR = Path.of("../core/src/test/resources/fixtures/java-with-unused-imports");

    @TempDir
    Path tempDir;

    private Path copyFixtureToTemp() throws IOException {
        Path source = FIXTURE_DIR.toAbsolutePath().normalize();
        for (String file : new String[] {"Example.java"}) {
            Path src = source.resolve(file);
            if (Files.exists(src)) {
                Files.copy(src, tempDir.resolve(file));
            }
        }
        return tempDir;
    }

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withValidRecipe_reportsChanges() throws IOException {
        Path workDir = copyFixtureToTemp();
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute(
                "run", "-r", "org.openrewrite.java.RemoveUnusedImports", "--project-dir", workDir.toString());

        assertThat(exitCode)
                .as("stderr: %s, stdout: %s", cli.stderr(), cli.stdout())
                .isZero();
    }

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withInvalidRecipe_reportsError() throws IOException {
        Path workDir = copyFixtureToTemp();
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("run", "-r", "nonexistent.recipe", "--project-dir", workDir.toString());

        assertThat(exitCode).isNotZero();
        String output = cli.stderr() + cli.stdout();
        assertThat(output).containsIgnoringCase("error");
    }

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withMissingRequiredOptions_fails() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("run");

        assertThat(exitCode).isNotZero();
    }
}

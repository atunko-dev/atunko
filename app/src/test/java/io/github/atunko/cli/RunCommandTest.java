package io.github.atunko.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunko.App;
import io.github.reqstool.annotations.SVCs;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class RunCommandTest {

    private static final Path FIXTURE_DIR = Path.of("../core/src/test/resources/fixtures/java-with-unused-imports");

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withValidRecipe_reportsChanges() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute(
                "run", "-r", "org.openrewrite.java.RemoveUnusedImports", "--project-dir", FIXTURE_DIR.toString());

        assertThat(exitCode).as("stderr: %s, stdout: %s", err, out).isZero();
    }

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withInvalidRecipe_reportsError() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("run", "-r", "nonexistent.recipe", "--project-dir", FIXTURE_DIR.toString());

        assertThat(exitCode).isNotZero();
        String output = err.toString() + out.toString();
        assertThat(output).containsIgnoringCase("error");
    }

    @Test
    @SVCs({"SVC_CLI_0003"})
    void run_withMissingRequiredOptions_fails() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("run");

        assertThat(exitCode).isNotZero();
    }
}

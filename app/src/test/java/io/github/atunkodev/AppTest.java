package io.github.atunkodev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AppTest {

    @Test
    void help_printsUsage() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("--help");

        assertThat(exitCode).isZero();
        String output = out.toString();
        assertThat(output).contains("atunko");
        assertThat(output).contains("discover");
        assertThat(output).contains("run");
    }

    @Test
    void noArgs_printsUsage() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute();

        assertThat(exitCode).isZero();
        String output = out.toString();
        assertThat(output).contains("atunko");
    }
}

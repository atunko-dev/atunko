package io.github.atunko.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunko.App;
import io.github.reqstool.annotations.SVCs;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DiscoverCommandTest {

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_listsAllRecipes() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("discover");

        assertThat(exitCode).isZero();
        String output = out.toString();
        assertThat(output).contains("recipe");
    }

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_withSearch_filtersRecipes() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("discover", "--search", "unused imports");

        assertThat(exitCode).isZero();
        String output = out.toString();
        assertThat(output).containsIgnoringCase("unused");
    }

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_withNoResults_showsMessage() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new App());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("discover", "--search", "xyznonexistent999");

        assertThat(exitCode).isZero();
        String output = out.toString();
        assertThat(output).contains("No recipes found");
    }
}

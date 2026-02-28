package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.Test;

class DiscoverCommandTest {

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_listsAllRecipes() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("discover");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe");
    }

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_withSearch_filtersRecipes() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("discover", "--search", "unused imports");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).containsIgnoringCase("unused");
    }

    @Test
    @SVCs({"SVC_CLI_0002"})
    void discover_withNoResults_showsMessage() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("discover", "--search", "xyznonexistent999");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("No recipes found");
    }
}

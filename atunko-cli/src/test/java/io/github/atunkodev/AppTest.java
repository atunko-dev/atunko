package io.github.atunkodev;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void helpPrintsUsage() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("atunko");
        assertThat(cli.stdout()).contains("list");
        assertThat(cli.stdout()).contains("search");
        assertThat(cli.stdout()).contains("run");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0001"})
    void tuiSubcommandIsRegistered() {
        CommandLineFixture cli = CommandLineFixture.create();

        assertThat(cli.cmd().getSubcommands()).containsKey("tui");
    }
}

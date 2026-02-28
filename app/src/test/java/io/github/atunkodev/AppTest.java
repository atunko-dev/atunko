package io.github.atunkodev;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void help_printsUsage() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("atunko");
        assertThat(cli.stdout()).contains("discover");
        assertThat(cli.stdout()).contains("run");
    }

    @Test
    void noArgs_printsUsage() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute();

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("atunko");
    }
}

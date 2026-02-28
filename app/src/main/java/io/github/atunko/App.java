package io.github.atunko;

import io.github.atunko.cli.DiscoverCommand;
import io.github.atunko.cli.RunCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "atunko",
        description = "OpenRewrite recipe discovery, execution, and configuration",
        mixinStandardHelpOptions = true,
        subcommands = {DiscoverCommand.class, RunCommand.class})
public class App implements Runnable {

    @Override
    public void run() {
        // Default: print usage (TUI launch deferred to CLI_0001)
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}

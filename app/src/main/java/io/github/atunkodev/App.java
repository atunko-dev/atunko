package io.github.atunkodev;

import io.github.atunkodev.cli.DiscoverCommand;
import io.github.atunkodev.cli.RunCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "atunko",
        description = "OpenRewrite recipe discovery, execution, and configuration",
        mixinStandardHelpOptions = true,
        subcommands = {DiscoverCommand.class, RunCommand.class})
public class App implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        // Default: print usage (TUI launch deferred to CLI_0001)
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}

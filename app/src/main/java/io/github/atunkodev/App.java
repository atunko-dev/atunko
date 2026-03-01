package io.github.atunkodev;

import io.github.atunkodev.cli.ErrorHandler;
import io.github.atunkodev.cli.ListCommand;
import io.github.atunkodev.cli.RunCommand;
import io.github.atunkodev.cli.SearchCommand;
import io.github.atunkodev.cli.ServiceFactory;
import io.github.atunkodev.tui.TuiCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "atunko",
        description = "OpenRewrite recipe browsing, execution, and configuration",
        mixinStandardHelpOptions = true,
        subcommands = {TuiCommand.class, ListCommand.class, SearchCommand.class, RunCommand.class})
public class App implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        // Default: print usage (TUI launch deferred to CLI_0001)
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App(), new ServiceFactory())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionExceptionHandler(new ErrorHandler())
                .execute(args);
        System.exit(exitCode);
    }
}

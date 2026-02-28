package io.github.atunkodev.testing;

import io.github.atunkodev.App;
import io.github.atunkodev.cli.ErrorHandler;
import io.github.atunkodev.cli.ServiceFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import picocli.CommandLine;

/** Reusable test fixture that wires a {@link CommandLine} with captured stdout/stderr and the standard ErrorHandler. */
public record CommandLineFixture(CommandLine cmd, StringWriter out, StringWriter err) {

    public static CommandLineFixture create() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new App(), new ServiceFactory());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        cmd.setExecutionExceptionHandler(new ErrorHandler());
        return new CommandLineFixture(cmd, out, err);
    }

    public int execute(String... args) {
        return cmd.execute(args);
    }

    public String stdout() {
        return out.toString();
    }

    public String stderr() {
        return err.toString();
    }
}

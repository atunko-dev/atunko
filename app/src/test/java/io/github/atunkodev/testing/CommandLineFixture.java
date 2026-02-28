package io.github.atunkodev.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.App;
import io.github.atunkodev.cli.ErrorHandler;
import io.github.atunkodev.cli.ServiceFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    public static void assertRecipeLinesAreSorted(String output) {
        String[] lines = output.split("\n");
        List<String> recipeLines =
                Arrays.stream(lines).filter(l -> l.matches("[a-zA-Z]\\S* - .*")).toList();
        assertThat(recipeLines).hasSizeGreaterThan(1);
        for (int i = 1; i < recipeLines.size(); i++) {
            assertThat(recipeLines.get(i).toLowerCase(Locale.ROOT))
                    .isGreaterThanOrEqualTo(recipeLines.get(i - 1).toLowerCase(Locale.ROOT));
        }
    }
}

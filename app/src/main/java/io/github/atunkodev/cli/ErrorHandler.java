package io.github.atunkodev.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/** Centralized error handler for CLI commands. Prints the error message to stderr and returns a non-zero exit code. */
public class ErrorHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        cmd.getErr().println("Error: " + ex.getMessage());
        cmd.getErr().flush();
        return cmd.getCommandSpec().exitCodeOnExecutionException();
    }
}

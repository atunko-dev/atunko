package io.github.atunkodev.cli;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.JavaSourceParser;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import org.openrewrite.SourceFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "run", description = "Execute an OpenRewrite recipe against a project", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {

    @Option(
            names = {"-r", "--recipe"},
            required = true,
            description = "Fully qualified recipe name")
    private String recipe;

    @Option(names = "--project-dir", required = true, description = "Path to the project directory")
    private Path projectDir;

    @Spec
    private CommandSpec spec;

    private final RecipeExecutionEngine engine;
    private final JavaSourceParser sourceParser;
    private final ChangeApplier changeApplier;

    public RunCommand() {
        this(new RecipeExecutionEngine(), new JavaSourceParser(), new ChangeApplier());
    }

    public RunCommand(RecipeExecutionEngine engine, JavaSourceParser sourceParser, ChangeApplier changeApplier) {
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
    }

    @Override
    @Requirements({"atunko:CLI_0003"})
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        List<SourceFile> sources = sourceParser.parse(projectDir);

        if (sources.isEmpty()) {
            out.println("No Java source files found in " + projectDir);
            out.flush();
            return;
        }

        ExecutionResult result = engine.execute(recipe, sources);

        if (result.changes().isEmpty()) {
            out.println("No changes produced by recipe: " + recipe);
        } else {
            changeApplier.apply(projectDir, result.changes());
            for (FileChange change : result.changes()) {
                out.println("Changed: " + change.path());
            }
            out.println("\n" + result.changes().size() + " file(s) changed.");
        }
        out.flush();
    }
}

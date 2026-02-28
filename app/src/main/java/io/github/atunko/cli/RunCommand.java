package io.github.atunko.cli;

import io.github.atunko.core.engine.ExecutionResult;
import io.github.atunko.core.engine.FileChange;
import io.github.atunko.core.engine.RecipeExecutionEngine;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
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

    @Override
    @Requirements({"CLI_0003"})
    public void run() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        try {
            List<SourceFile> sources = parseJavaSources(projectDir);

            if (sources.isEmpty()) {
                out.println("No Java source files found in " + projectDir);
                out.flush();
                return;
            }

            RecipeExecutionEngine engine = new RecipeExecutionEngine();
            ExecutionResult result = engine.execute(recipe, sources);

            if (result.changes().isEmpty()) {
                out.println("No changes produced by recipe: " + recipe);
            } else {
                for (FileChange change : result.changes()) {
                    out.println("Changed: " + change.path());
                }
                out.println("\n" + result.changes().size() + " file(s) changed.");
            }
            out.flush();
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
            err.flush();
            throw new picocli.CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private List<SourceFile> parseJavaSources(Path dir) throws IOException {
        Path absoluteDir = dir.toAbsolutePath().normalize();
        List<Path> javaFiles;
        try (Stream<Path> walk = Files.walk(absoluteDir)) {
            javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }

        return JavaParser.fromJavaVersion()
                .build()
                .parse(javaFiles, absoluteDir, new InMemoryExecutionContext())
                .toList();
    }
}

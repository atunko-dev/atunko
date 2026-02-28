package io.github.atunkodev.cli;

import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
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
            Path absoluteDir = projectDir.toAbsolutePath().normalize();
            for (FileChange change : result.changes()) {
                applyChange(absoluteDir, change);
                out.println("Changed: " + change.path());
            }
            out.println("\n" + result.changes().size() + " file(s) changed.");
        }
        out.flush();
    }

    private void applyChange(Path absoluteDir, FileChange change) {
        try {
            if (change.after() != null) {
                Files.writeString(absoluteDir.resolve(change.path()), change.after());
            } else {
                Files.deleteIfExists(absoluteDir.resolve(change.path()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<SourceFile> parseJavaSources(Path dir) {
        Path absoluteDir = dir.toAbsolutePath().normalize();

        if (!Files.isDirectory(absoluteDir)) {
            throw new IllegalArgumentException("Not a valid directory: " + absoluteDir);
        }

        try {
            List<Path> javaFiles;
            try (Stream<Path> walk = Files.walk(absoluteDir)) {
                javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
            }

            return JavaParser.fromJavaVersion()
                    .build()
                    .parse(javaFiles, absoluteDir, new InMemoryExecutionContext())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

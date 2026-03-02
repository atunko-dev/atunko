package io.github.atunkodev.core.project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;

public class JavaSourceParser {

    public List<SourceFile> parse(Path projectDir) {
        Path absoluteDir = projectDir.toAbsolutePath().normalize();

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

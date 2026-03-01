package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.json.JsonParser;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;

public class ProjectSourceParser {

    private static final Set<String> JAVA_EXTENSIONS = Set.of("java");
    private static final Set<String> XML_EXTENSIONS = Set.of("xml", "xsd", "xsl", "xslt", "wsdl", "tld");
    private static final Set<String> YAML_EXTENSIONS = Set.of("yml", "yaml");
    private static final Set<String> JSON_EXTENSIONS = Set.of("json");
    private static final Set<String> PROPERTIES_EXTENSIONS = Set.of("properties");

    @Requirements({"CORE_0003"})
    public List<SourceFile> parse(ProjectInfo projectInfo) {
        List<Path> rawDirs = projectInfo.allSourceAndResourceDirs();
        if (rawDirs.isEmpty()) {
            rawDirs = projectInfo.sourceDirs();
        }
        List<Path> allDirs =
                rawDirs.stream().map(d -> d.toAbsolutePath().normalize()).toList();

        Map<String, List<Path>> filesByExtension = collectFilesByExtension(allDirs);
        ExecutionContext ctx = new InMemoryExecutionContext();
        Path relativeTo = findRelativeTo(allDirs);
        List<SourceFile> allSources = new ArrayList<>();

        parseJavaFiles(filesByExtension, projectInfo.classpath(), relativeTo, ctx, allSources);
        parseWithBuilder(filesByExtension, XML_EXTENSIONS, XmlParser.builder(), relativeTo, ctx, allSources);
        parseWithBuilder(filesByExtension, YAML_EXTENSIONS, YamlParser.builder(), relativeTo, ctx, allSources);
        parseWithBuilder(filesByExtension, JSON_EXTENSIONS, JsonParser.builder(), relativeTo, ctx, allSources);
        parseWithBuilder(
                filesByExtension, PROPERTIES_EXTENSIONS, PropertiesParser.builder(), relativeTo, ctx, allSources);

        return List.copyOf(allSources);
    }

    private Map<String, List<Path>> collectFilesByExtension(List<Path> dirs) {
        Map<String, List<Path>> filesByExtension = new LinkedHashMap<>();
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    String ext = extension(p);
                    if (!ext.isEmpty()) {
                        filesByExtension
                                .computeIfAbsent(ext, k -> new ArrayList<>())
                                .add(p);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return filesByExtension;
    }

    private void parseJavaFiles(
            Map<String, List<Path>> filesByExtension,
            List<Path> classpath,
            Path relativeTo,
            ExecutionContext ctx,
            List<SourceFile> allSources) {
        List<Path> javaFiles = collectFiles(filesByExtension, JAVA_EXTENSIONS);
        if (!javaFiles.isEmpty()) {
            JavaParser.Builder<?, ?> builder = JavaParser.fromJavaVersion();
            if (classpath != null && !classpath.isEmpty()) {
                builder.classpath(classpath);
            }
            allSources.addAll(builder.build().parse(javaFiles, relativeTo, ctx).toList());
        }
    }

    private void parseWithBuilder(
            Map<String, List<Path>> filesByExtension,
            Set<String> extensions,
            Parser.Builder builder,
            Path relativeTo,
            ExecutionContext ctx,
            List<SourceFile> allSources) {
        List<Path> files = collectFiles(filesByExtension, extensions);
        if (!files.isEmpty()) {
            allSources.addAll(builder.build().parse(files, relativeTo, ctx).toList());
        }
    }

    private List<Path> collectFiles(Map<String, List<Path>> filesByExtension, Set<String> extensions) {
        List<Path> files = new ArrayList<>();
        for (String ext : extensions) {
            files.addAll(filesByExtension.getOrDefault(ext, List.of()));
        }
        return files;
    }

    private Path findRelativeTo(List<Path> dirs) {
        if (dirs.isEmpty()) {
            return Path.of(".").toAbsolutePath().normalize();
        }
        Path first = dirs.getFirst().toAbsolutePath().normalize();
        Path candidate = first;
        for (Path dir : dirs) {
            Path abs = dir.toAbsolutePath().normalize();
            while (!abs.startsWith(candidate)) {
                candidate = candidate.getParent();
                if (candidate == null) {
                    return Path.of(".").toAbsolutePath().normalize();
                }
            }
        }
        return candidate;
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "";
    }
}

package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;

public class ProjectSourceParser {

    /** Files with this exact name are parsed by {@link MavenParser} rather than {@link XmlParser}. */
    private static final String POM_FILE_NAME = "pom.xml";

    private static final Set<String> JAVA_EXTENSIONS = Set.of("java");
    private static final Set<String> XML_EXTENSIONS = Set.of("xml", "xsd", "xsl", "xslt", "wsdl", "tld");
    private static final Set<String> YAML_EXTENSIONS = Set.of("yml", "yaml");
    private static final Set<String> JSON_EXTENSIONS = Set.of("json");
    private static final Set<String> PROPERTIES_EXTENSIONS = Set.of("properties");

    @Requirements({"atunko:CORE_0003", "atunko:CORE_0003.1"})
    public List<SourceFile> parse(ProjectInfo projectInfo) {
        return parseWithCapabilities(projectInfo).sources();
    }

    /**
     * Parses the project and additionally reports which {@link SourceCapability} the parsed source set provides.
     *
     * <p>A capability is reported only when the corresponding parser actually produced at least one source file.
     * {@link SourceCapability#MAVEN} is reported when at least one {@code pom.xml} was parsed by {@link MavenParser}
     * into a document carrying a {@code MavenResolutionResult} marker — never merely because a {@code pom.xml} exists.
     * {@link SourceCapability#GRADLE} is never reported: Gradle build files are not parsed at all.
     */
    @Requirements({"atunko:CORE_0003", "atunko:CORE_0003.1", "atunko:CORE_0015.1", "atunko:CORE_0016"})
    public ParsedSources parseWithCapabilities(ProjectInfo projectInfo) {
        List<Path> allDirs = projectInfo.parseRoots();

        Map<String, List<Path>> filesByExtension = collectFilesByExtension(allDirs);
        List<Path> pomFiles = collectPomFiles(filesByExtension, projectInfo.buildFiles());
        ExecutionContext ctx = new InMemoryExecutionContext();
        Path relativeTo = findRelativeTo(allDirs, pomFiles);
        List<SourceFile> allSources = new ArrayList<>();
        Set<SourceCapability> capabilities = EnumSet.noneOf(SourceCapability.class);

        if (parseJavaFiles(filesByExtension, projectInfo.classpath(), relativeTo, ctx, allSources)) {
            capabilities.add(SourceCapability.JAVA);
        }
        boolean pomsParsed = !pomFiles.isEmpty();
        if (parsePomFiles(pomFiles, relativeTo, ctx, allSources)) {
            capabilities.add(SourceCapability.MAVEN);
        }
        boolean otherXmlParsed = parseWithBuilder(
                filesByExtension, XML_EXTENSIONS, pomFiles, XmlParser.builder(), relativeTo, ctx, allSources);
        // A pom.xml is an XML document whichever parser produced it, so it counts towards the XML capability.
        addCapabilityIfParsed(capabilities, SourceCapability.XML, pomsParsed || otherXmlParsed);
        addCapabilityIfParsed(
                capabilities,
                SourceCapability.YAML,
                parseWithBuilder(
                        filesByExtension,
                        YAML_EXTENSIONS,
                        List.of(),
                        YamlParser.builder(),
                        relativeTo,
                        ctx,
                        allSources));
        addCapabilityIfParsed(
                capabilities,
                SourceCapability.JSON,
                parseWithBuilder(
                        filesByExtension,
                        JSON_EXTENSIONS,
                        List.of(),
                        JsonParser.builder(),
                        relativeTo,
                        ctx,
                        allSources));
        addCapabilityIfParsed(
                capabilities,
                SourceCapability.PROPERTIES,
                parseWithBuilder(
                        filesByExtension,
                        PROPERTIES_EXTENSIONS,
                        List.of(),
                        PropertiesParser.builder(),
                        relativeTo,
                        ctx,
                        allSources));

        return new ParsedSources(allSources, capabilities);
    }

    private static void addCapabilityIfParsed(
            Set<SourceCapability> capabilities, SourceCapability capability, boolean parsed) {
        if (parsed) {
            capabilities.add(capability);
        }
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

    /** @return {@code true} when at least one source file was produced */
    private boolean parseJavaFiles(
            Map<String, List<Path>> filesByExtension,
            List<Path> classpath,
            Path relativeTo,
            ExecutionContext ctx,
            List<SourceFile> allSources) {
        List<Path> javaFiles = collectFiles(filesByExtension, JAVA_EXTENSIONS);
        if (javaFiles.isEmpty()) {
            return false;
        }
        JavaParser.Builder<?, ?> builder = JavaParser.fromJavaVersion();
        if (classpath != null && !classpath.isEmpty()) {
            builder.classpath(classpath);
        }
        List<SourceFile> parsed =
                builder.build().parse(javaFiles, relativeTo, ctx).toList();
        allSources.addAll(parsed);
        return !parsed.isEmpty();
    }

    /**
     * Collects the {@code pom.xml} files of the project: those found while walking the source and resource
     * directories, plus any declared as {@link ProjectInfo#buildFiles()} (a Maven project's poms normally live above
     * its source directories, so the scanner has to point at them explicitly). Duplicates are removed and order is
     * stable so that the parent pom of a multi-module build is parsed alongside its modules.
     */
    @Requirements({"atunko:CORE_0016.1"})
    private List<Path> collectPomFiles(Map<String, List<Path>> filesByExtension, List<Path> buildFiles) {
        Set<Path> poms = new LinkedHashSet<>();
        for (Path candidate : filesByExtension.getOrDefault("xml", List.of())) {
            if (isPom(candidate)) {
                poms.add(candidate.toAbsolutePath().normalize());
            }
        }
        for (Path candidate : buildFiles) {
            if (isPom(candidate) && Files.isRegularFile(candidate)) {
                poms.add(candidate.toAbsolutePath().normalize());
            }
        }
        return List.copyOf(poms);
    }

    private static boolean isPom(Path path) {
        return POM_FILE_NAME.equals(path.getFileName().toString());
    }

    /**
     * Parses every {@code pom.xml} of the project in a single {@link MavenParser} call so that parent/child
     * relationships within a multi-module build resolve against each other rather than against a repository.
     *
     * <p>A pom only counts as Maven-parsed when the resulting document carries a {@link MavenResolutionResult}
     * marker — that marker is what every {@code org.openrewrite.maven.*} recipe requires. Poms that failed to resolve
     * (unresolvable parent, missing dependency, offline run) are re-parsed with {@link XmlParser} so that they are
     * still present as plain XML and carry no parse-failure markers.
     *
     * @return {@code true} when at least one pom was parsed into a Maven resolution model
     */
    @Requirements({"atunko:CORE_0016", "atunko:CORE_0016.1", "atunko:CORE_0016.2"})
    private boolean parsePomFiles(
            List<Path> pomFiles, Path relativeTo, ExecutionContext ctx, List<SourceFile> allSources) {
        if (pomFiles.isEmpty()) {
            return false;
        }
        List<SourceFile> mavenParsed;
        try {
            // Swallow resolution errors: a failed pom is handled by falling back to XML, not by failing the parse.
            ExecutionContext mavenCtx = new InMemoryExecutionContext(t -> {});
            mavenParsed = MavenParser.builder()
                    .build()
                    .parse(pomFiles, relativeTo, mavenCtx)
                    .toList();
        } catch (RuntimeException e) {
            parseAsPlainXml(pomFiles, relativeTo, ctx, allSources);
            return false;
        }

        List<Path> unresolved = new ArrayList<>();
        boolean anyResolved = false;
        for (SourceFile parsed : mavenParsed) {
            if (parsed.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                allSources.add(parsed);
                anyResolved = true;
            } else {
                unresolved.add(relativeTo.resolve(parsed.getSourcePath()));
            }
        }
        parseAsPlainXml(unresolved, relativeTo, ctx, allSources);
        return anyResolved;
    }

    private void parseAsPlainXml(List<Path> files, Path relativeTo, ExecutionContext ctx, List<SourceFile> allSources) {
        if (files.isEmpty()) {
            return;
        }
        allSources.addAll(
                XmlParser.builder().build().parse(files, relativeTo, ctx).toList());
    }

    /**
     * @param excluded files that another parser has already claimed (the project's poms)
     * @return {@code true} when at least one source file was produced
     */
    private boolean parseWithBuilder(
            Map<String, List<Path>> filesByExtension,
            Set<String> extensions,
            List<Path> excluded,
            Parser.Builder builder,
            Path relativeTo,
            ExecutionContext ctx,
            List<SourceFile> allSources) {
        List<Path> files = collectFiles(filesByExtension, extensions);
        if (!excluded.isEmpty()) {
            Set<Path> skip = Set.copyOf(excluded);
            files = files.stream()
                    .filter(f -> !skip.contains(f.toAbsolutePath().normalize()))
                    .toList();
        }
        if (files.isEmpty()) {
            return false;
        }
        List<SourceFile> parsed = builder.build().parse(files, relativeTo, ctx).toList();
        allSources.addAll(parsed);
        return !parsed.isEmpty();
    }

    private List<Path> collectFiles(Map<String, List<Path>> filesByExtension, Set<String> extensions) {
        List<Path> files = new ArrayList<>();
        for (String ext : extensions) {
            files.addAll(filesByExtension.getOrDefault(ext, List.of()));
        }
        return files;
    }

    /**
     * Common ancestor of everything that will be parsed. Build files typically live above the source directories (a
     * module's {@code pom.xml} sits next to its {@code src/}), so their parent directories take part in the
     * computation — otherwise source paths could not be relativized against the result.
     */
    private Path findRelativeTo(List<Path> sourceDirs, List<Path> pomFiles) {
        List<Path> dirs = new ArrayList<>(sourceDirs);
        for (Path pom : pomFiles) {
            Path parent = pom.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                dirs.add(parent);
            }
        }
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

package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0018"})
class ParsedSourcesCacheTest {

    /** Counts parses and returns a fresh {@link ParsedSources} per call so cache hits are identity-checkable. */
    private static final class CountingParser extends ProjectSourceParser {
        private final AtomicInteger parses = new AtomicInteger();

        @Override
        public ParsedSources parseWithCapabilities(ProjectInfo projectInfo) {
            parses.incrementAndGet();
            return new ParsedSources(List.of(), Set.of(SourceCapability.JAVA));
        }
    }

    @TempDir
    Path projectDir;

    private CountingParser parser;
    private ParsedSourcesCache cache;
    private Path srcDir;
    private Path buildFile;

    @BeforeEach
    void setUp() throws IOException {
        parser = new CountingParser();
        cache = new ParsedSourcesCache(parser, true);
        srcDir = Files.createDirectories(projectDir.resolve("src"));
        Files.writeString(srcDir.resolve("A.java"), "class A {}");
        buildFile = Files.writeString(projectDir.resolve("pom.xml"), "<project/>");
    }

    private ProjectInfo info() {
        return new ProjectInfo(List.of(), List.of(srcDir), List.of(), List.of(), List.of(), List.of(buildFile));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018"})
    void unchangedProjectIsServedFromCache() {
        ParsedSources first = cache.get(projectDir, info());
        ParsedSources second = cache.get(projectDir, info());

        assertThat(parser.parses).hasValue(1);
        assertThat(second).isSameAs(first);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.1"})
    void modifiedSourceFileTriggersReparse() throws IOException {
        cache.get(projectDir, info());
        Files.writeString(srcDir.resolve("A.java"), "class A { int changed; }");

        cache.get(projectDir, info());

        assertThat(parser.parses).hasValue(2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.1"})
    void addedSourceFileTriggersReparse() throws IOException {
        cache.get(projectDir, info());
        Files.writeString(srcDir.resolve("B.java"), "class B {}");

        cache.get(projectDir, info());

        assertThat(parser.parses).hasValue(2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.1"})
    void removedSourceFileTriggersReparse() throws IOException {
        cache.get(projectDir, info());
        Files.delete(srcDir.resolve("A.java"));

        cache.get(projectDir, info());

        assertThat(parser.parses).hasValue(2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.1"})
    void modifiedBuildFileTriggersReparse() throws IOException {
        cache.get(projectDir, info());
        Files.writeString(buildFile, "<project><!-- changed --></project>");

        cache.get(projectDir, info());

        assertThat(parser.parses).hasValue(2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.1"})
    void changedProjectInfoTriggersReparse() throws IOException {
        cache.get(projectDir, info());
        Path jar = Files.writeString(projectDir.resolve("dep.jar"), "");
        ProjectInfo withClasspath =
                new ProjectInfo(List.of(jar), List.of(srcDir), List.of(), List.of(), List.of(), List.of(buildFile));

        cache.get(projectDir, withClasspath);

        assertThat(parser.parses).hasValue(2);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.2"})
    void workspaceProjectsAreCachedIndependently(@TempDir Path otherDir) throws IOException {
        Path otherSrc = Files.createDirectories(otherDir.resolve("src"));
        Files.writeString(otherSrc.resolve("C.java"), "class C {}");
        ProjectInfo otherInfo = new ProjectInfo(List.of(), List.of(otherSrc));

        cache.get(projectDir, info());
        cache.get(otherDir, otherInfo);
        assertThat(parser.parses).hasValue(2);

        // A change in the first project must not evict the second.
        Files.writeString(srcDir.resolve("A.java"), "class A { int changed; }");
        cache.get(projectDir, info());
        cache.get(otherDir, otherInfo);

        assertThat(parser.parses).hasValue(3);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0018.3"})
    void disabledCacheParsesEveryCall() {
        ParsedSourcesCache disabled = new ParsedSourcesCache(parser, false);

        disabled.get(projectDir, info());
        disabled.get(projectDir, info());

        assertThat(parser.parses).hasValue(2);
    }
}

package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

class ProjectSourceParserTest {

    private final ProjectSourceParser parser = new ProjectSourceParser();

    private static final Path FIXTURE_DIR = Path.of("src/test/resources/fixtures/multi-file-project");

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesJavaFiles() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(FIXTURE_DIR.resolve("src/main/java")), List.of(), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).isNotEmpty();
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".java"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesYamlFiles() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(), List.of(FIXTURE_DIR.resolve("src/main/resources")), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".yml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesPropertiesFiles() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(), List.of(FIXTURE_DIR.resolve("src/main/resources")), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".properties"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesXmlFiles() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(), List.of(FIXTURE_DIR.resolve("src/main/resources")), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".xml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesJsonFiles() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(), List.of(FIXTURE_DIR.resolve("src/main/resources")), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".json"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseParsesAllFileTypesFromMultipleDirectories() {
        ProjectInfo info = new ProjectInfo(
                List.of(),
                List.of(FIXTURE_DIR.resolve("src/main/java")),
                List.of(FIXTURE_DIR.resolve("src/main/resources")),
                List.of(),
                List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).hasSizeGreaterThanOrEqualTo(5);
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".java"));
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".yml"));
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".properties"));
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".json"));
        assertThat(sources).anyMatch(s -> s.getSourcePath().toString().endsWith(".xml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.1"})
    void parseEmptyDirectoriesReturnsEmptyList() {
        ProjectInfo info = new ProjectInfo(List.of(), List.of(), List.of(), List.of(), List.of());

        List<SourceFile> sources = parser.parse(info);

        assertThat(sources).isEmpty();
    }
}

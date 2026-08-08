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

    @Test
    @SVCs({"atunko:SVC_CORE_0015.1"})
    void parseWithCapabilitiesReportsCapabilitiesOfWhatWasParsed() {
        ProjectInfo info = new ProjectInfo(
                List.of(),
                List.of(FIXTURE_DIR.resolve("src/main/java")),
                List.of(FIXTURE_DIR.resolve("src/main/resources")),
                List.of(),
                List.of());

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.sources())
                .extracting(SourceFile::getSourcePath)
                .containsExactlyElementsOf(parser.parse(info).stream()
                        .map(SourceFile::getSourcePath)
                        .toList());
        assertThat(parsed.capabilities())
                .contains(
                        SourceCapability.JAVA,
                        SourceCapability.XML,
                        SourceCapability.YAML,
                        SourceCapability.JSON,
                        SourceCapability.PROPERTIES);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.1"})
    void parseWithCapabilitiesOmitsCapabilitiesForFileTypesNotPresent() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(FIXTURE_DIR.resolve("src/main/java")), List.of(), List.of(), List.of());

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.capabilities()).containsExactly(SourceCapability.JAVA);
        assertThat(parsed.has(SourceCapability.JAVA)).isTrue();
        assertThat(parsed.has(SourceCapability.YAML)).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.1"})
    void parseWithCapabilitiesNeverReportsMavenOrGradleForAMavenProject() {
        // Stage 1: pom.xml is parsed as plain XML and Gradle build files are not parsed at all,
        // so neither build-model capability may be claimed.
        Path mavenFixture = Path.of("src/test/resources/fixtures/maven-project");
        ProjectInfo info = new ProjectInfo(List.of(), List.of(mavenFixture), List.of(), List.of(), List.of());

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.capabilities()).doesNotContain(SourceCapability.MAVEN, SourceCapability.GRADLE);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.1"})
    void parseWithCapabilitiesOfEmptyProjectHasNoCapabilities() {
        ProjectInfo info = new ProjectInfo(List.of(), List.of(), List.of(), List.of(), List.of());

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.sources()).isEmpty();
        assertThat(parsed.capabilities()).isEmpty();
    }
}

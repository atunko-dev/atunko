package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.recipe.RecipeApplicabilityService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.tree.Xml;

/**
 * Stage 2 of recipe applicability: {@code pom.xml} is parsed by OpenRewrite's {@code MavenParser}, which is what gives
 * the parsed documents the {@code MavenResolutionResult} marker that every {@code org.openrewrite.maven.*} recipe
 * requires.
 *
 * <p>All fixtures used here are deliberately free of external dependencies and of external parent poms, so the
 * assertions hold without network access. The only fixture that needs a repository lookup is the deliberately broken
 * one, and there the expected outcome (resolution fails) is the same online and offline.
 */
class ProjectSourceParserMavenTest {

    private static final Path MULTI_MODULE = Path.of("src/test/resources/fixtures/maven-multi-module");
    private static final Path BROKEN = Path.of("src/test/resources/fixtures/maven-broken-project");
    private static final Path MULTI_FILE = Path.of("src/test/resources/fixtures/multi-file-project");

    private final ProjectSourceParser parser = new ProjectSourceParser();

    @Test
    @SVCs({"atunko:SVC_CORE_0016"})
    void pomIsParsedByMavenParserAndCarriesAResolutionMarker() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));

        SourceFile rootPom = pom(parsed, "pom.xml");
        assertThat(rootPom).isInstanceOf(Xml.Document.class);
        assertThat(rootPom.getMarkers().findFirst(MavenResolutionResult.class)).isPresent();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016"})
    void mavenCapabilityIsReportedForAMavenProject() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));

        assertThat(parsed.capabilities()).contains(SourceCapability.MAVEN, SourceCapability.XML);
        assertThat(parsed.has(SourceCapability.MAVEN)).isTrue();
        assertThat(parsed.has(SourceCapability.GRADLE)).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.1"})
    void allPomsOfAMultiModuleBuildAreParsedInOneCallAndAllResolve() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));

        List<SourceFile> poms = parsed.sources().stream()
                .filter(s -> s.getSourcePath().getFileName().toString().equals("pom.xml"))
                .toList();

        assertThat(poms).hasSize(3);
        assertThat(poms)
                .allSatisfy(p -> assertThat(p.getMarkers().findFirst(MavenResolutionResult.class))
                        .as("resolution marker on %s", p.getSourcePath())
                        .isPresent());
        assertThat(poms)
                .extracting(s -> s.getSourcePath().toString())
                .containsExactlyInAnyOrder("pom.xml", "lib/pom.xml", "app/pom.xml");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.1"})
    void moduleParentRelationshipsResolveAgainstEachOther() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));

        MavenResolutionResult app = pom(parsed, "app/pom.xml")
                .getMarkers()
                .findFirst(MavenResolutionResult.class)
                .orElseThrow();

        assertThat(app.getParent()).isNotNull();
        assertThat(app.getParent().getPom().getArtifactId()).isEqualTo("multi-module-fixture");
        assertThat(app.getPom().getProperties()).containsEntry("maven.compiler.release", "17");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016"})
    void aRealMavenRecipeProducesChangesOnTheParsedSources() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));

        // OrderPomElements is a MavenIsoVisitor recipe with no required options and no repository access:
        // it only acts on documents that carry a MavenResolutionResult marker.
        ExecutionResult result =
                new RecipeExecutionEngine().execute("org.openrewrite.maven.OrderPomElements", parsed.sources());

        assertThat(result.changes()).isNotEmpty();
        assertThat(result.changes())
                .anySatisfy(change -> assertThat(change.path().toString()).isEqualTo("app/pom.xml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016"})
    void mavenRecipesAreApplicableOnceTheMavenCapabilityIsPresent() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(MULTI_MODULE));
        RecipeApplicabilityService service = new RecipeApplicabilityService();
        RecipeInfo mavenRecipe = leafRecipe("org.openrewrite.maven.OrderPomElements");

        assertThat(service.applicability(mavenRecipe, parsed.capabilities()).applicable())
                .isTrue();
        assertThat(service.applicability(mavenRecipe, java.util.Set.of(SourceCapability.XML))
                        .applicable())
                .isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.2"})
    void anUnresolvablePomFallsBackToPlainXmlWithoutTheMavenCapability() {
        ParsedSources parsed = parser.parseWithCapabilities(projectAt(BROKEN));

        assertThat(parsed.capabilities()).contains(SourceCapability.XML);
        assertThat(parsed.capabilities()).doesNotContain(SourceCapability.MAVEN);

        SourceFile brokenPom = pom(parsed, "pom.xml");
        assertThat(brokenPom).isInstanceOf(Xml.Document.class);
        assertThat(brokenPom).isNotInstanceOf(ParseError.class);
        assertThat(brokenPom.getMarkers().findFirst(MavenResolutionResult.class))
                .isEmpty();
        assertThat(markerTypeNames(brokenPom.getMarkers())).noneMatch(n -> n.contains("ParseExceptionResult"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016"})
    void xmlFilesThatAreNotPomsStillGoThroughTheXmlParser() {
        ProjectInfo info = new ProjectInfo(
                List.of(), List.of(), List.of(MULTI_FILE.resolve("src/main/resources")), List.of(), List.of());

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.capabilities()).contains(SourceCapability.XML);
        assertThat(parsed.capabilities()).doesNotContain(SourceCapability.MAVEN);
        assertThat(parsed.sources())
                .anySatisfy(s -> assertThat(s.getSourcePath().toString()).endsWith(".xml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.1"})
    void pomsDeclaredAsBuildFilesArePickedUpEvenWhenOutsideTheSourceDirectories() {
        ProjectInfo info = new ProjectInfo(
                List.of(),
                List.of(MULTI_MODULE.resolve("app")),
                List.of(),
                List.of(),
                List.of(),
                List.of(MULTI_MODULE.resolve("pom.xml"), MULTI_MODULE.resolve("app/pom.xml")));

        ParsedSources parsed = parser.parseWithCapabilities(info);

        assertThat(parsed.capabilities()).contains(SourceCapability.MAVEN);
        assertThat(parsed.sources())
                .extracting(s -> s.getSourcePath().toString())
                .contains("pom.xml", "app/pom.xml");
    }

    private static ProjectInfo projectAt(Path dir) {
        return new ProjectInfo(List.of(), List.of(dir));
    }

    private static SourceFile pom(ParsedSources parsed, String sourcePath) {
        return parsed.sources().stream()
                .filter(s -> s.getSourcePath().toString().equals(sourcePath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no parsed source at " + sourcePath + " in "
                        + parsed.sources().stream()
                                .map(s -> s.getSourcePath().toString())
                                .toList()));
    }

    private static List<String> markerTypeNames(Markers markers) {
        return markers.getMarkers().stream().map(m -> m.getClass().getName()).toList();
    }

    private static RecipeInfo leafRecipe(String name) {
        return new RecipeInfo(name, name, "", java.util.Set.of());
    }
}

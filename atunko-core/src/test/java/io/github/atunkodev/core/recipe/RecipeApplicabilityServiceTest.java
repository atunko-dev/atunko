package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.project.SourceCapability;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeApplicabilityServiceTest {

    private static final Set<SourceCapability> JAVA_ONLY = Set.of(SourceCapability.JAVA, SourceCapability.XML);

    private final RecipeApplicabilityService service = new RecipeApplicabilityService();

    private static RecipeInfo leaf(String name) {
        return new RecipeInfo(name, name, "desc", Set.of());
    }

    private static RecipeInfo composite(String name, RecipeInfo... children) {
        return new RecipeInfo(name, name, "desc", Set.of(), List.of(children));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015"})
    void mavenRecipeIsInapplicableWithoutMavenCapability() {
        RecipeInfo recipe = leaf("org.openrewrite.maven.ChangePropertyValue");

        RecipeApplicability result = service.applicability(recipe, JAVA_ONLY);

        assertThat(result.applicable()).isFalse();
        assertThat(result.missingCapability()).contains(SourceCapability.MAVEN);
        assertThat(result.reason()).contains("Maven");
        assertThat(result.badgeLabel()).isEqualTo("needs Maven");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015"})
    void gradleRecipeIsInapplicableWithoutGradleCapability() {
        RecipeInfo recipe = leaf("org.openrewrite.gradle.UpgradeDependencyVersion");

        RecipeApplicability result = service.applicability(recipe, JAVA_ONLY);

        assertThat(result.applicable()).isFalse();
        assertThat(result.missingCapability()).contains(SourceCapability.GRADLE);
        assertThat(result.badgeLabel()).isEqualTo("needs Gradle");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015"})
    void mavenRecipeIsApplicableWhenMavenCapabilityIsPresent() {
        RecipeInfo recipe = leaf("org.openrewrite.maven.ChangePropertyValue");

        RecipeApplicability result =
                service.applicability(recipe, Set.of(SourceCapability.JAVA, SourceCapability.MAVEN));

        assertThat(result.applicable()).isTrue();
        assertThat(result.missingCapability()).isEmpty();
        assertThat(result.reason()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.1"})
    void nonBuildRecipesAreAlwaysApplicable() {
        assertThat(service.applicability(leaf("org.openrewrite.java.RemoveUnusedImports"), JAVA_ONLY)
                        .applicable())
                .isTrue();
        assertThat(service.applicability(leaf("org.openrewrite.yaml.ChangeKey"), Set.of())
                        .applicable())
                .isTrue();
        // a recipe whose name merely contains "maven" is not a maven-prefixed recipe
        assertThat(service.applicability(leaf("org.openrewrite.java.dependencies.maven.Something"), JAVA_ONLY)
                        .applicable())
                .isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.2"})
    void compositeIsApplicableWhenAnyTransitiveChildIsApplicable() {
        RecipeInfo inner = composite(
                "org.example.Inner", leaf("org.openrewrite.maven.A"), leaf("org.openrewrite.java.RemoveUnusedImports"));
        RecipeInfo outer = composite("org.example.Outer", leaf("org.openrewrite.gradle.B"), inner);

        assertThat(service.applicability(outer, JAVA_ONLY).applicable()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.2"})
    void compositeIsInapplicableWhenNoTransitiveChildIsApplicable() {
        RecipeInfo inner = composite("org.example.Inner", leaf("org.openrewrite.maven.A"));
        RecipeInfo outer = composite("org.example.Outer", leaf("org.openrewrite.gradle.B"), inner);

        RecipeApplicability result = service.applicability(outer, JAVA_ONLY);

        assertThat(result.applicable()).isFalse();
        assertThat(result.reason()).isNotEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.2"})
    void compositeWithOnlyMavenChildrenReportsMavenAsMissingCapability() {
        RecipeInfo outer =
                composite("org.example.MavenOnly", leaf("org.openrewrite.maven.A"), leaf("org.openrewrite.maven.B"));

        RecipeApplicability result = service.applicability(outer, JAVA_ONLY);

        assertThat(result.applicable()).isFalse();
        assertThat(result.missingCapability()).contains(SourceCapability.MAVEN);
        assertThat(result.badgeLabel()).isEqualTo("needs Maven");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.2"})
    void cyclicCompositeDoesNotRecurseForever() {
        RecipeInfo child = leaf("org.openrewrite.maven.A");
        RecipeInfo cyclic = composite("org.example.Cyclic", child);
        // a composite that contains a composite of the same name — the traversal must terminate
        RecipeInfo outer = composite("org.example.Cyclic", cyclic, leaf("org.openrewrite.gradle.B"));

        assertThat(service.applicability(outer, JAVA_ONLY).applicable()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.3"})
    void resultsAreCachedPerCapabilitySet() {
        RecipeInfo recipe = leaf("org.openrewrite.maven.ChangePropertyValue");

        RecipeApplicability first = service.applicability(recipe, JAVA_ONLY);
        RecipeApplicability second = service.applicability(recipe, JAVA_ONLY);
        assertThat(second).isSameAs(first);

        RecipeApplicability withMaven = service.applicability(recipe, Set.of(SourceCapability.MAVEN));
        assertThat(withMaven.applicable()).isTrue();
        // the earlier cache entry is untouched
        assertThat(service.applicability(recipe, JAVA_ONLY)).isSameAs(first);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0015.3"})
    void capabilitySetOrderDoesNotAffectCacheKey() {
        RecipeInfo recipe = leaf("org.openrewrite.maven.ChangePropertyValue");

        RecipeApplicability first = service.applicability(recipe, Set.of(SourceCapability.JAVA, SourceCapability.XML));
        RecipeApplicability second = service.applicability(recipe, Set.of(SourceCapability.XML, SourceCapability.JAVA));

        assertThat(second).isSameAs(first);
    }
}

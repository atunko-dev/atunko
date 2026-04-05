package io.github.atunkodev.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeFilterTest {

    private static final RecipeInfo SPRING =
            new RecipeInfo("o.t.Spring", "Spring Migrate", "Migrate spring boot apps", Set.of("java", "spring"));
    private static final RecipeInfo KOTLIN =
            new RecipeInfo("o.t.Kotlin", "Kotlin Recipe", "Kotlin-only transformation", Set.of("kotlin"));
    private static final RecipeInfo UNICODE =
            new RecipeInfo("o.t.Unicode", "Café λ", "Unicode recipe description", Set.of("java"));

    // --- matchesText ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_nullQuery_returnsTrue() {
        assertThat(RecipeFilter.matchesText(SPRING, null)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_blankQuery_returnsTrue() {
        assertThat(RecipeFilter.matchesText(SPRING, "")).isTrue();
        assertThat(RecipeFilter.matchesText(SPRING, "   ")).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_caseInsensitive_matches() {
        assertThat(RecipeFilter.matchesText(SPRING, "SPRING")).isTrue();
        assertThat(RecipeFilter.matchesText(SPRING, "migrate")).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_multiWordQuery_requiresAllWords() {
        assertThat(RecipeFilter.matchesText(SPRING, "migrate boot")).isTrue();
        assertThat(RecipeFilter.matchesText(SPRING, "migrate kotlin")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_partialWord_matches() {
        assertThat(RecipeFilter.matchesText(SPRING, "sprin")).isTrue();
        assertThat(RecipeFilter.matchesText(SPRING, "migrat")).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_noMatch_returnsFalse() {
        assertThat(RecipeFilter.matchesText(SPRING, "groovy")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_unicodeQuery_matches() {
        assertThat(RecipeFilter.matchesText(UNICODE, "Café")).isTrue();
        assertThat(RecipeFilter.matchesText(UNICODE, "λ")).isTrue();
    }

    // --- matchesTags ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_nullTags_returnsTrue() {
        assertThat(RecipeFilter.matchesTags(SPRING, null)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_emptyTags_returnsTrue() {
        assertThat(RecipeFilter.matchesTags(SPRING, Set.of())).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_singleTagMatch_returnsTrue() {
        assertThat(RecipeFilter.matchesTags(SPRING, Set.of("java"))).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_singleTagNoMatch_returnsFalse() {
        assertThat(RecipeFilter.matchesTags(SPRING, Set.of("kotlin"))).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_multipleTagsOrLogic_returnsTrue() {
        // SPRING has "java" — filter includes "spring" and "java" — should match (OR logic)
        assertThat(RecipeFilter.matchesTags(SPRING, Set.of("spring", "kotlin"))).isTrue();
        // KOTLIN has only "kotlin" — filter includes "java" only — should not match
        assertThat(RecipeFilter.matchesTags(KOTLIN, Set.of("java"))).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesTags_noTagMatch_returnsFalse() {
        assertThat(RecipeFilter.matchesTags(SPRING, Set.of("groovy", "scala"))).isFalse();
    }

    // --- filter ---

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void filter_emptyFilters_returnsAll() {
        List<RecipeInfo> recipes = List.of(SPRING, KOTLIN, UNICODE);
        assertThat(RecipeFilter.filter(recipes, null, null)).containsExactlyInAnyOrder(SPRING, KOTLIN, UNICODE);
        assertThat(RecipeFilter.filter(recipes, "", Set.of())).containsExactlyInAnyOrder(SPRING, KOTLIN, UNICODE);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void filter_noMatches_returnsEmpty() {
        List<RecipeInfo> recipes = List.of(SPRING, KOTLIN, UNICODE);
        assertThat(RecipeFilter.filter(recipes, "groovy", Set.of())).isEmpty();
        assertThat(RecipeFilter.filter(recipes, "", Set.of("groovy"))).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.2"})
    void matchesText_fullyQualifiedName_matches() {
        assertThat(RecipeFilter.matchesText(SPRING, "o.t.Spring")).isTrue();
        assertThat(RecipeFilter.matchesText(SPRING, "o.t.Kotlin")).isFalse();
    }
}

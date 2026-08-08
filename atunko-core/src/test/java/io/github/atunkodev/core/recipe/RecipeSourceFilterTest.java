package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.Test;

class RecipeSourceFilterTest {

    @Test
    @SVCs({"atunko:SVC_CORE_0019.2"})
    void matchesFiltersBySource() {
        assertThat(RecipeSourceFilter.ALL.matches(RecipeSource.BUNDLED)).isTrue();
        assertThat(RecipeSourceFilter.ALL.matches(RecipeSource.USER)).isTrue();
        assertThat(RecipeSourceFilter.BUNDLED.matches(RecipeSource.BUNDLED)).isTrue();
        assertThat(RecipeSourceFilter.BUNDLED.matches(RecipeSource.USER)).isFalse();
        assertThat(RecipeSourceFilter.USER.matches(RecipeSource.USER)).isTrue();
        assertThat(RecipeSourceFilter.USER.matches(RecipeSource.BUNDLED)).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019.2"})
    void nextCyclesAllBundledUserAndBackToAll() {
        assertThat(RecipeSourceFilter.ALL.next()).isEqualTo(RecipeSourceFilter.BUNDLED);
        assertThat(RecipeSourceFilter.BUNDLED.next()).isEqualTo(RecipeSourceFilter.USER);
        assertThat(RecipeSourceFilter.USER.next()).isEqualTo(RecipeSourceFilter.ALL);
    }
}

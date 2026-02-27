package io.github.atunko.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

@SVCs({"SVC_CORE_0001"})
class RecipeDiscoveryServiceTest {

    private final RecipeDiscoveryService service = new RecipeDiscoveryService();

    @Test
    void discoverAll_returnsNonEmptyList() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    void discoverAll_recipesHaveNameAndDescription() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).allSatisfy(recipe -> {
            assertThat(recipe.name()).isNotBlank();
            assertThat(recipe.displayName()).isNotBlank();
        });
    }

    @Test
    void discoverAll_includesKnownRecipe() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).extracting(RecipeInfo::name).contains("org.openrewrite.java.RemoveUnusedImports");
    }
}

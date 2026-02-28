package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeDiscoveryServiceTest {

    private final RecipeDiscoveryService service = new RecipeDiscoveryService();

    @Test
    @SVCs({"SVC_CORE_0001"})
    void discoverAll_returnsNonEmptyList() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    @SVCs({"SVC_CORE_0001"})
    void discoverAll_recipesHaveNameAndDescription() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).allSatisfy(recipe -> {
            assertThat(recipe.name()).isNotBlank();
            assertThat(recipe.displayName()).isNotBlank();
        });
    }

    @Test
    @SVCs({"SVC_CORE_0001"})
    void discoverAll_includesKnownRecipe() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).extracting(RecipeInfo::name).contains("org.openrewrite.java.RemoveUnusedImports");
    }

    @Test
    @SVCs({"SVC_CORE_0002"})
    void search_withMatchingKeyword_returnsFilteredRecipes() {
        List<RecipeInfo> results = service.search("RemoveUnusedImports");
        assertThat(results).isNotEmpty();
        assertThat(results)
                .allSatisfy(recipe -> assertThat(recipe.name() + recipe.displayName() + recipe.description())
                        .containsIgnoringCase("RemoveUnusedImports"));
    }

    @Test
    @SVCs({"SVC_CORE_0002"})
    void search_isCaseInsensitive() {
        List<RecipeInfo> upper = service.search("REMOVEUNUSEDIMPORTS");
        List<RecipeInfo> lower = service.search("removeunusedimports");
        List<RecipeInfo> mixed = service.search("RemoveUnusedImports");
        assertThat(upper).isEqualTo(lower);
        assertThat(lower).isEqualTo(mixed);
        assertThat(upper).isNotEmpty();
    }

    @Test
    @SVCs({"SVC_CORE_0002"})
    void search_matchesAgainstNameDisplayNameDescriptionAndTags() {
        // Match against name (fully qualified recipe name)
        assertThat(service.search("org.openrewrite.java.RemoveUnusedImports")).isNotEmpty();

        // Match against displayName
        assertThat(service.search("Remove unused imports")).isNotEmpty();

        // Match against description — search for a common word in recipe descriptions
        assertThat(service.search("import")).isNotEmpty();

        // Match against tags — "java" is a common tag
        List<RecipeInfo> tagResults = service.search("java");
        assertThat(tagResults).isNotEmpty();
        assertThat(tagResults)
                .anyMatch(recipe ->
                        recipe.tags().stream().anyMatch(tag -> tag.toLowerCase().contains("java")));
    }

    @Test
    @SVCs({"SVC_CORE_0002"})
    void search_withNonMatchingKeyword_returnsEmptyList() {
        List<RecipeInfo> results = service.search("zzz_no_recipe_matches_this_xyzzy");
        assertThat(results).isEmpty();
    }

    @Test
    @SVCs({"SVC_CORE_0002"})
    void search_withBlankOrNullQuery_returnsAllRecipes() {
        List<RecipeInfo> all = service.discoverAll();
        assertThat(service.search(null)).isEqualTo(all);
        assertThat(service.search("")).isEqualTo(all);
        assertThat(service.search("   ")).isEqualTo(all);
    }
}

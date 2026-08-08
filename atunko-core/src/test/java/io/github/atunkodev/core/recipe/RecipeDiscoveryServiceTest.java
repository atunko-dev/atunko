package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeDiscoveryServiceTest {

    private final RecipeDiscoveryService service = new RecipeDiscoveryService();

    @Test
    @SVCs({"atunko:SVC_CORE_0001"})
    void discoverAllReturnsNonEmptyList() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0001"})
    void discoverAllRecipesHaveNameAndDescription() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).allSatisfy(recipe -> {
            assertThat(recipe.name()).isNotBlank();
            assertThat(recipe.displayName()).isNotBlank();
        });
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0001"})
    void discoverAllIncludesKnownRecipe() {
        List<RecipeInfo> recipes = service.discoverAll();
        assertThat(recipes).extracting(RecipeInfo::name).contains("org.openrewrite.java.RemoveUnusedImports");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0002"})
    void searchWithMatchingKeywordReturnsFilteredRecipes() {
        List<RecipeInfo> results = service.search("RemoveUnusedImports");
        assertThat(results).isNotEmpty();
        assertThat(results)
                .allSatisfy(recipe -> assertThat(recipe.name() + recipe.displayName() + recipe.description())
                        .containsIgnoringCase("RemoveUnusedImports"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0002"})
    void searchIsCaseInsensitive() {
        List<RecipeInfo> upper = service.search("REMOVEUNUSEDIMPORTS");
        List<RecipeInfo> lower = service.search("removeunusedimports");
        List<RecipeInfo> mixed = service.search("RemoveUnusedImports");
        assertThat(upper).isEqualTo(lower);
        assertThat(lower).isEqualTo(mixed);
        assertThat(upper).isNotEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0002"})
    void searchMatchesAgainstNameDisplayNameDescriptionAndTags() {
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
                .anyMatch(recipe -> recipe.tags().stream()
                        .anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains("java")));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0002"})
    void searchWithNonMatchingKeywordReturnsEmptyList() {
        List<RecipeInfo> results = service.search("zzz_no_recipe_matches_this_xyzzy");
        assertThat(results).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0002"})
    void searchWithBlankOrNullQueryReturnsAllRecipes() {
        List<RecipeInfo> all = service.discoverAll();
        assertThat(service.search(null)).isEqualTo(all);
        assertThat(service.search("")).isEqualTo(all);
        assertThat(service.search("   ")).isEqualTo(all);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0001.1"})
    void discoverAllCompositeRecipesExposeSubRecipes() {
        List<RecipeInfo> recipes = service.discoverAll();

        List<RecipeInfo> composites =
                recipes.stream().filter(RecipeInfo::isComposite).toList();
        assertThat(composites).isNotEmpty();
        assertThat(composites).allSatisfy(recipe -> {
            assertThat(recipe.recipeList()).isNotEmpty();
            assertThat(recipe.recipeList())
                    .allSatisfy(sub -> assertThat(sub.name()).isNotBlank());
        });
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0001.1"})
    void discoverAllNonCompositeRecipesHaveEmptyRecipeList() {
        List<RecipeInfo> recipes = service.discoverAll();

        List<RecipeInfo> nonComposites =
                recipes.stream().filter(r -> !r.isComposite()).toList();
        assertThat(nonComposites).isNotEmpty();
        assertThat(nonComposites)
                .allSatisfy(recipe -> assertThat(recipe.recipeList()).isEmpty());
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019"})
    void discoverAllClassifiesClasspathRecipesAsBundled() {
        assertThat(service.discoverAll())
                .isNotEmpty()
                .allSatisfy(recipe -> assertThat(recipe.source()).isEqualTo(RecipeSource.BUNDLED));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019"})
    void recipeInfoConstructorsDefaultToBundledSource() {
        RecipeInfo fourArg = new RecipeInfo("n", "dn", "d", Set.of());
        RecipeInfo fiveArg = new RecipeInfo("n", "dn", "d", Set.of(), List.of());
        RecipeInfo sixArg = new RecipeInfo("n", "dn", "d", Set.of(), List.of(), List.of());

        assertThat(fourArg.source()).isEqualTo(RecipeSource.BUNDLED);
        assertThat(fiveArg.source()).isEqualTo(RecipeSource.BUNDLED);
        assertThat(sixArg.source()).isEqualTo(RecipeSource.BUNDLED);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019.2"})
    void discoverAllWithFilterOnBundledOnlyCatalog() {
        List<RecipeInfo> all = service.discoverAll();

        assertThat(service.discoverAll(RecipeSourceFilter.ALL)).isEqualTo(all);
        assertThat(service.discoverAll(RecipeSourceFilter.BUNDLED)).isEqualTo(all);
        assertThat(service.discoverAll(RecipeSourceFilter.USER)).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0014"})
    void discoverAllPopulatesOptionMetadata() {
        List<RecipeInfo> recipes = service.discoverAll();

        // At least some recipes should have options (OpenRewrite ships many with options)
        boolean anyHasOptions = recipes.stream().anyMatch(r -> !r.options().isEmpty());
        assertThat(anyHasOptions).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0014.1"})
    void optionInfoFieldsArePopulated() {
        List<RecipeInfo> recipes = service.discoverAll();

        // Find the first recipe that has options to verify RecipeOptionInfo field mapping
        RecipeInfo withOptions = recipes.stream()
                .filter(r -> !r.options().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No recipe with options found"));

        withOptions.options().forEach(opt -> {
            assertThat(opt.name()).isNotBlank();
            assertThat(opt.type()).isNotBlank();
            assertThat(opt.displayName()).isNotBlank();
        });
    }
}

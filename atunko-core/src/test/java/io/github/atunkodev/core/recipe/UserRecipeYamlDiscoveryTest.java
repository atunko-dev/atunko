package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Discovery against an environment that includes user-authored recipe YAML files — one valid declarative recipe
 * referencing a bundled recipe, plus one malformed file that must be skipped with a warning instead of crashing the
 * environment build.
 */
@SVCs({"atunko:SVC_CORE_0020"})
class UserRecipeYamlDiscoveryTest {

    private static final String USER_RECIPE_NAME = "io.github.atunkodev.test.UserYamlRecipe";
    private static final String BUNDLED_SUB_RECIPE_NAME = "org.openrewrite.java.RemoveUnusedImports";

    // The environment build performs a full classpath scan, so it is shared across all tests in this class.
    private static EnvironmentProvider provider;
    private static RecipeDiscoveryService service;
    private static Path malformedFile;

    @BeforeAll
    static void buildEnvironmentWithUserRecipeFiles(@TempDir Path tempDir) throws IOException {
        Path valid = tempDir.resolve("my-recipes.yml");
        Files.writeString(valid, """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: User YAML test recipe
            description: A declarative recipe authored as a plain YAML file for testing.
            tags:
              - user-yaml-test
            recipeList:
              - %s
            """.formatted(USER_RECIPE_NAME, BUNDLED_SUB_RECIPE_NAME));
        malformedFile = tempDir.resolve("broken.yml");
        Files.writeString(malformedFile, "type: specs.openrewrite.org/v1beta/recipe\nname: [unclosed\n  ::: not yaml");
        provider = new EnvironmentProvider(List.of(), List.of(valid, malformedFile));
        service = new RecipeDiscoveryService(provider);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020"})
    void yamlFileRecipeIsDiscoveredAndClassifiedUser() {
        RecipeInfo userRecipe = service.discoverAll().stream()
                .filter(r -> USER_RECIPE_NAME.equals(r.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User recipe file recipe was not discovered"));

        assertThat(userRecipe.source()).isEqualTo(RecipeSource.USER);
        assertThat(provider.userRecipeNames()).containsExactly(USER_RECIPE_NAME);
        assertThat(provider.isUserRecipe(USER_RECIPE_NAME)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020"})
    void bundledRecipesStayBundledIncludingSubRecipesOfUserYamlComposites() {
        List<RecipeInfo> all = service.discoverAll();

        RecipeInfo userRecipe = all.stream()
                .filter(r -> USER_RECIPE_NAME.equals(r.name()))
                .findFirst()
                .orElseThrow();
        assertThat(userRecipe.recipeList()).anySatisfy(sub -> {
            assertThat(sub.name()).isEqualTo(BUNDLED_SUB_RECIPE_NAME);
            assertThat(sub.source()).isEqualTo(RecipeSource.BUNDLED);
        });

        assertThat(all.stream().filter(r -> !USER_RECIPE_NAME.equals(r.name())))
                .isNotEmpty()
                .allSatisfy(r -> assertThat(r.source()).isEqualTo(RecipeSource.BUNDLED));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020.3"})
    void malformedYamlFileIsSkippedWithAWarningNamingTheFile() {
        assertThat(provider.loadWarnings()).hasSize(1);
        assertThat(provider.loadWarnings().get(0))
                .contains(malformedFile.toString())
                .contains("Skipping malformed recipe file");
        // The valid file still loaded despite the malformed neighbour.
        assertThat(provider.userRecipeNames()).containsExactly(USER_RECIPE_NAME);
    }
}

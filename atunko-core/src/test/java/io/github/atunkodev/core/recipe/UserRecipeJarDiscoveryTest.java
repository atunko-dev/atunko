package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Discovery against an environment that includes a user-supplied recipe jar. The jar is built on the fly and contains
 * one declarative recipe referencing a bundled recipe, mirroring how teams ship their own recipe libraries.
 */
@SVCs({"atunko:SVC_CORE_0019"})
class UserRecipeJarDiscoveryTest {

    private static final String USER_RECIPE_NAME = "io.github.atunkodev.test.UserSuppliedRecipe";
    private static final String BUNDLED_SUB_RECIPE_NAME = "org.openrewrite.java.RemoveUnusedImports";

    // The environment build performs a full classpath scan, so it is shared across all tests in this class.
    private static EnvironmentProvider provider;
    private static RecipeDiscoveryService service;

    @BeforeAll
    static void buildEnvironmentWithUserJar(@TempDir Path tempDir) throws IOException {
        Path jar = createUserRecipeJar(tempDir);
        provider = new EnvironmentProvider(List.of(jar));
        service = new RecipeDiscoveryService(provider);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019.1"})
    void userJarRecipeIsDiscoveredAndClassifiedUser() {
        RecipeInfo userRecipe = service.discoverAll().stream()
                .filter(r -> USER_RECIPE_NAME.equals(r.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User jar recipe was not discovered"));

        assertThat(userRecipe.source()).isEqualTo(RecipeSource.USER);
        assertThat(provider.userRecipeNames()).containsExactly(USER_RECIPE_NAME);
        assertThat(provider.isUserRecipe(USER_RECIPE_NAME)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019.1"})
    void bundledRecipesStayBundledIncludingSubRecipesOfUserComposites() {
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
    @SVCs({"atunko:SVC_CORE_0019.2"})
    void discoverAllFiltersBySource() {
        List<RecipeInfo> all = service.discoverAll(RecipeSourceFilter.ALL);
        List<RecipeInfo> bundled = service.discoverAll(RecipeSourceFilter.BUNDLED);
        List<RecipeInfo> user = service.discoverAll(RecipeSourceFilter.USER);

        assertThat(all).isEqualTo(service.discoverAll());
        assertThat(user).extracting(RecipeInfo::name).containsExactly(USER_RECIPE_NAME);
        assertThat(bundled).extracting(RecipeInfo::name).doesNotContain(USER_RECIPE_NAME);
        assertThat(bundled).hasSize(all.size() - 1);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0019.2"})
    void searchFiltersBySource() {
        var fields = java.util.Set.of(RecipeField.values());

        assertThat(service.search("UserSuppliedRecipe", fields, RecipeSourceFilter.USER))
                .extracting(RecipeInfo::name)
                .containsExactly(USER_RECIPE_NAME);
        assertThat(service.search("UserSuppliedRecipe", fields, RecipeSourceFilter.BUNDLED))
                .isEmpty();
    }

    private static Path createUserRecipeJar(Path dir) throws IOException {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: User supplied test recipe
            description: A declarative recipe contributed from a user jar for testing.
            tags:
              - user-test
            recipeList:
              - %s
            """.formatted(USER_RECIPE_NAME, BUNDLED_SUB_RECIPE_NAME);
        Path jar = dir.resolve("user-recipes.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/rewrite/user-recipes.yml"));
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }
}

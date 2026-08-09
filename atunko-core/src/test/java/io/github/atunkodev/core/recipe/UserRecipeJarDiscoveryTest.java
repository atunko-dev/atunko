package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.RecipeJarFixture;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Discovery against an environment that includes a user-supplied recipe jar. The jar is built on the fly and contains
 * one declarative recipe referencing a bundled recipe, mirroring how teams ship their own recipe libraries.
 */
@SVCs({"atunko:SVC_CORE_0019"})
class UserRecipeJarDiscoveryTest {

    private static final String USER_RECIPE_NAME = RecipeJarFixture.USER_RECIPE_NAME;
    private static final String BUNDLED_SUB_RECIPE_NAME = RecipeJarFixture.BUNDLED_SUB_RECIPE_NAME;

    // The environment build performs a full classpath scan, so it is shared across all tests in this class.
    private static EnvironmentProvider provider;
    private static RecipeDiscoveryService service;

    @BeforeAll
    static void buildEnvironmentWithUserJar(@TempDir Path tempDir) throws IOException {
        Path jar = RecipeJarFixture.create(tempDir);
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

    /** A user jar redeclaring a bundled recipe name must neither duplicate the catalog entry nor reclassify it. */
    @Test
    @SVCs({"atunko:SVC_CORE_0019.1"})
    void userJarRedeclaringBundledNameNeitherDuplicatesNorReclassifies(@TempDir Path dir) throws IOException {
        Path colliding =
                RecipeJarFixture.create(dir, BUNDLED_SUB_RECIPE_NAME, "org.openrewrite.java.format.AutoFormat");
        RecipeDiscoveryService collidingService =
                new RecipeDiscoveryService(new EnvironmentProvider(List.of(colliding)));

        List<RecipeInfo> matches = collidingService.discoverAll().stream()
                .filter(r -> BUNDLED_SUB_RECIPE_NAME.equals(r.name()))
                .toList();

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().source()).isEqualTo(RecipeSource.BUNDLED);
    }
}

package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.atunkodev.testing.RecipeJarFixture;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CLI_0002"})
class ListCommandTest {

    @Test
    @SVCs({"atunko:SVC_CLI_0002.1"})
    void listDisplaysRecipesAsText() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe");
        assertThat(cli.stdout()).contains(" - ");
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0002.2"})
    void listDisplaysRecipesAsJson() throws Exception {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--format", "json");

        assertThat(exitCode).isZero();
        String output = cli.stdout().trim();
        assertThat(output).startsWith("[");
        assertThat(output).endsWith("]");

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> recipes = mapper.readValue(output, new TypeReference<>() {});
        assertThat(recipes).isNotEmpty();
        assertThat(recipes.get(0)).containsKeys("name", "displayName", "description", "tags");
        assertThat(recipes.get(0)).doesNotContainKey("options");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0002.3"})
    void listSortsByNameByDefault() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "name");

        assertThat(exitCode).isZero();
        CommandLineFixture.assertRecipeLinesAreSorted(cli.stdout());
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0002.4"})
    void listSortsByTags() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0008"})
    void listSourceBundledEqualsUnfilteredAndSourceUserIsEmptyWithoutJars() {
        CommandLineFixture unfiltered = CommandLineFixture.create();
        unfiltered.execute("list");

        CommandLineFixture bundled = CommandLineFixture.create();
        int bundledExit = bundled.execute("list", "--source", "bundled");
        assertThat(bundledExit).isZero();
        assertThat(bundled.stdout()).isEqualTo(unfiltered.stdout());

        CommandLineFixture user = CommandLineFixture.create();
        int userExit = user.execute("list", "--source", "user");
        assertThat(userExit).isZero();
        assertThat(user.stdout()).contains("No recipes found.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0008.1"})
    void listWithRecipeJarAndSourceUserListsOnlyUserRecipes(@TempDir Path tempDir) throws Exception {
        Path jar = RecipeJarFixture.create(tempDir);
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--recipe-jar", jar.toString(), "--source", "user");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains(RecipeJarFixture.USER_RECIPE_NAME);
        assertThat(cli.stdout()).contains("1 recipe(s) found.");
    }

    /** A mistyped jar path must fail loudly, not scan past it and report an empty catalog with exit 0. */
    @Test
    @SVCs({"atunko:SVC_CLI_0008.1"})
    void listWithNonexistentRecipeJarFails(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("no-such.jar");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--recipe-jar", missing.toString());

        assertThat(exitCode).isNotZero();
        assertThat(cli.stderr()).contains("Recipe jar not found").contains("no-such.jar");
    }
}

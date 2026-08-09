package io.github.atunkodev.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds a user recipe YAML file on the fly: one declarative recipe referencing a bundled recipe, mirroring how users
 * author their own recipes without packaging them into a jar.
 */
public final class RecipeYamlFixture {

    public static final String USER_RECIPE_NAME = "io.github.atunkodev.test.UserYamlRecipe";

    private RecipeYamlFixture() {}

    public static Path create(Path dir) throws IOException {
        Path file = dir.resolve("user-recipes.yml");
        Files.writeString(file, """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: User YAML test recipe
            description: A declarative recipe authored as a plain YAML file for testing.
            tags:
              - user-yaml-test
            recipeList:
              - org.openrewrite.java.RemoveUnusedImports
            """.formatted(USER_RECIPE_NAME));
        return file;
    }

    /** A file that is not valid recipe YAML — it must be skipped with a warning, never crash the command. */
    public static Path createMalformed(Path dir) throws IOException {
        Path file = dir.resolve("broken-recipes.yml");
        Files.writeString(file, "type: specs.openrewrite.org/v1beta/recipe\nname: [unclosed\n  ::: not yaml");
        return file;
    }
}

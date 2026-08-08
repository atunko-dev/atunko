package io.github.atunkodev.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Builds a user recipe jar on the fly: one declarative recipe referencing a bundled recipe, mirroring how teams ship
 * their own recipe libraries.
 */
public final class RecipeJarFixture {

    public static final String USER_RECIPE_NAME = "io.github.atunkodev.test.UserSuppliedRecipe";

    private RecipeJarFixture() {}

    public static Path create(Path dir) throws IOException {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: User supplied test recipe
            description: A declarative recipe contributed from a user jar for testing.
            tags:
              - user-test
            recipeList:
              - org.openrewrite.java.RemoveUnusedImports
            """.formatted(USER_RECIPE_NAME);
        Path jar = dir.resolve("user-recipes.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/rewrite/user-recipes.yml"));
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }
}

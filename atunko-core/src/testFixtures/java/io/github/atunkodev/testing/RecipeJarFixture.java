package io.github.atunkodev.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Builds a user recipe jar on the fly: one declarative recipe referencing a bundled recipe, mirroring how teams ship
 * their own recipe libraries. Shared between core and CLI tests via test fixtures so the recipe-jar format is
 * defined exactly once.
 */
public final class RecipeJarFixture {

    public static final String USER_RECIPE_NAME = "io.github.atunkodev.test.UserSuppliedRecipe";

    /** The bundled recipe the fixture's declarative recipe delegates to. */
    public static final String BUNDLED_SUB_RECIPE_NAME = "org.openrewrite.java.RemoveUnusedImports";

    private RecipeJarFixture() {}

    public static Path create(Path dir) throws IOException {
        return create(dir, USER_RECIPE_NAME, BUNDLED_SUB_RECIPE_NAME);
    }

    /**
     * Builds a jar declaring {@code recipeName} delegating to {@code subRecipeName} — for example a jar that
     * redeclares a bundled recipe name to exercise collision handling.
     */
    public static Path create(Path dir, String recipeName, String subRecipeName) throws IOException {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: User supplied test recipe
            description: A declarative recipe contributed from a user jar for testing.
            tags:
              - user-test
            recipeList:
              - %s
            """.formatted(recipeName, subRecipeName);
        Path jar = dir.resolve("user-recipes-" + Integer.toHexString(recipeName.hashCode()) + ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/rewrite/user-recipes.yml"));
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }
}

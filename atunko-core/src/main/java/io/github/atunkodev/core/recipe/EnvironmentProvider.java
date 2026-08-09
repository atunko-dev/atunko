package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;

/**
 * Lazy-initialized, thread-safe provider for the OpenRewrite {@link Environment}. The Environment is immutable once
 * built, so it is safe to cache and share across callers.
 *
 * <p>The environment always contains the bundled catalog scanned from atunko's own runtime classpath. Optionally it
 * also loads user-supplied recipe jars; the recipe names contributed by those jars are tracked so that discovery can
 * classify each recipe's {@link RecipeSource} honestly — {@code USER} exactly when the recipe came from a user jar.
 * A user jar redeclaring a name that also exists in the bundled catalog does not reclassify the bundled recipe:
 * such collisions stay {@code BUNDLED}, so `--source bundled` never silently drops a bundled recipe.
 */
public class EnvironmentProvider {

    private record Scan(Environment environment, Set<String> bundledRecipeNames, Set<String> userRecipeNames) {}

    private final List<Path> userRecipeJars;
    private volatile Scan scan;

    public EnvironmentProvider() {
        this(List.of());
    }

    public EnvironmentProvider(List<Path> userRecipeJars) {
        this.userRecipeJars = List.copyOf(userRecipeJars);
    }

    public Environment get() {
        return scan().environment();
    }

    /** Names of the recipes contributed by user-supplied recipe jars — empty when none were configured. */
    @Requirements({"atunko:CORE_0019.1"})
    public Set<String> userRecipeNames() {
        return scan().userRecipeNames();
    }

    /**
     * Whether the named recipe was contributed by a user-supplied recipe jar. A name that also exists in the
     * bundled catalog is not a user recipe — the bundled classification wins for collisions.
     */
    @Requirements({"atunko:CORE_0019.1"})
    public boolean isUserRecipe(String recipeName) {
        Scan s = scan();
        return s.userRecipeNames().contains(recipeName)
                && !s.bundledRecipeNames().contains(recipeName);
    }

    public void invalidate() {
        scan = null;
    }

    private Scan scan() {
        Scan result = scan;
        if (result == null) {
            synchronized (this) {
                result = scan;
                if (result == null) {
                    result = build();
                    scan = result;
                }
            }
        }
        return result;
    }

    @Requirements({"atunko:CORE_0019.1"})
    private Scan build() {
        // Same loader that Environment.Builder.scanRuntimeClasspath() creates — kept explicit so it can serve as
        // the dependency loader for user jars, letting declarative user recipes reference bundled recipes.
        ClasspathScanningLoader bundled = new ClasspathScanningLoader(new Properties(), new String[0]);
        Environment.Builder builder = Environment.builder().load(bundled);
        Set<String> bundledNames = new LinkedHashSet<>();
        bundled.listRecipeDescriptors().stream().map(RecipeDescriptor::getName).forEach(bundledNames::add);
        Set<String> userNames = new LinkedHashSet<>();
        for (Path jar : userRecipeJars) {
            ClasspathScanningLoader userLoader =
                    new ClasspathScanningLoader(jar, new Properties(), List.of(bundled), classLoaderFor(jar));
            builder.load(userLoader, List.of(bundled));
            userLoader.listRecipeDescriptors().stream()
                    .map(RecipeDescriptor::getName)
                    .forEach(userNames::add);
        }
        return new Scan(builder.build(), Set.copyOf(bundledNames), Set.copyOf(userNames));
    }

    // The classloader must stay open for the lifetime of the environment: imperative user recipes are instantiated
    // through it whenever they run.
    private static ClassLoader classLoaderFor(Path jar) {
        try {
            return new URLClassLoader(new URL[] {jar.toUri().toURL()}, EnvironmentProvider.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid recipe jar path: " + jar, e);
        }
    }
}

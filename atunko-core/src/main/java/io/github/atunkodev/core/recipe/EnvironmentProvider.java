package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

/**
 * Lazy-initialized, thread-safe provider for the OpenRewrite {@link Environment}. The Environment is immutable once
 * built, so it is safe to cache and share across callers.
 *
 * <p>The environment always contains the bundled catalog scanned from atunko's own runtime classpath. Optionally it
 * also loads user-supplied recipe jars and user-authored declarative recipe YAML files; the recipe names contributed
 * by those sources are tracked so that discovery can classify each recipe's {@link RecipeSource} honestly —
 * {@code USER} exactly when the recipe came from a user jar or a user recipe file. A user source redeclaring a name
 * that also exists in the bundled catalog does not reclassify the bundled recipe: such collisions stay
 * {@code BUNDLED}, so `--source bundled` never silently drops a bundled recipe.
 *
 * <p>Recipe YAML files are hand-edited, so they get per-file failure isolation: a malformed file is skipped with a
 * warning (see {@link #loadWarnings()}) and never crashes the environment build.
 */
public class EnvironmentProvider {

    private static final Logger LOG = Logger.getLogger(EnvironmentProvider.class.getName());

    private record Scan(
            Environment environment,
            Set<String> bundledRecipeNames,
            Set<String> userRecipeNames,
            List<String> warnings) {}

    private final List<Path> userRecipeJars;
    private final List<Path> userRecipeFiles;
    private volatile Scan scan;

    public EnvironmentProvider() {
        this(List.of());
    }

    public EnvironmentProvider(List<Path> userRecipeJars) {
        this(userRecipeJars, List.of());
    }

    public EnvironmentProvider(List<Path> userRecipeJars, List<Path> userRecipeFiles) {
        this.userRecipeJars = List.copyOf(userRecipeJars);
        this.userRecipeFiles = List.copyOf(userRecipeFiles);
    }

    public Environment get() {
        return scan().environment();
    }

    /** Names of the recipes contributed by user-supplied recipe jars or files — empty when none were configured. */
    @Requirements({"atunko:CORE_0019.1"})
    public Set<String> userRecipeNames() {
        return scan().userRecipeNames();
    }

    /**
     * Whether the named recipe was contributed by a user-supplied recipe jar or file. A name that also exists in
     * the bundled catalog is not a user recipe — the bundled classification wins for collisions.
     */
    @Requirements({"atunko:CORE_0019.1"})
    public boolean isUserRecipe(String recipeName) {
        Scan s = scan();
        return s.userRecipeNames().contains(recipeName)
                && !s.bundledRecipeNames().contains(recipeName);
    }

    /** Warnings collected while loading user recipe files — one per skipped file, naming file and cause. */
    @Requirements({"atunko:CORE_0020.3"})
    public List<String> loadWarnings() {
        return scan().warnings();
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

    @Requirements({"atunko:CORE_0019.1", "atunko:CORE_0020"})
    private Scan build() {
        // Same loader that Environment.Builder.scanRuntimeClasspath() creates — kept explicit so it can serve as
        // the dependency loader for user jars and files, letting declarative user recipes reference bundled recipes.
        ClasspathScanningLoader bundled = new ClasspathScanningLoader(new Properties(), new String[0]);
        Environment.Builder builder = Environment.builder().load(bundled);
        Set<String> bundledNames = new LinkedHashSet<>();
        bundled.listRecipeDescriptors().stream().map(RecipeDescriptor::getName).forEach(bundledNames::add);
        Set<String> userNames = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        for (Path jar : userRecipeJars) {
            ClasspathScanningLoader userLoader =
                    new ClasspathScanningLoader(jar, new Properties(), List.of(bundled), classLoaderFor(jar));
            builder.load(userLoader, List.of(bundled));
            userLoader.listRecipeDescriptors().stream()
                    .map(RecipeDescriptor::getName)
                    .forEach(userNames::add);
        }
        for (Path file : userRecipeFiles) {
            loadRecipeFile(file, bundled, builder, userNames, warnings);
        }
        return new Scan(builder.build(), Set.copyOf(bundledNames), Set.copyOf(userNames), List.copyOf(warnings));
    }

    /**
     * Loads one user recipe YAML file with per-file failure isolation: parsing and descriptor listing both happen
     * before the loader joins the environment builder, so a malformed file is skipped with a warning instead of
     * poisoning the build.
     */
    @Requirements({"atunko:CORE_0020", "atunko:CORE_0020.3"})
    private static void loadRecipeFile(
            Path file,
            ClasspathScanningLoader bundled,
            Environment.Builder builder,
            Set<String> userNames,
            List<String> warnings) {
        try (InputStream in = Files.newInputStream(file)) {
            YamlResourceLoader loader = new YamlResourceLoader(
                    in, file.toUri(), new Properties(), EnvironmentProvider.class.getClassLoader(), List.of(bundled));
            List<String> names = loader.listRecipeDescriptors().stream()
                    .map(RecipeDescriptor::getName)
                    .toList();
            builder.load(loader, List.of(bundled));
            userNames.addAll(names);
        } catch (IOException | RuntimeException e) {
            String warning = "Skipping malformed recipe file " + file + ": " + describe(e);
            warnings.add(warning);
            LOG.warning(warning);
        }
    }

    private static String describe(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
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

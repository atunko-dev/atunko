package io.github.atunkodev.core;

import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.recipe.EnvironmentProvider;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.UserRecipeFiles;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the discovery service and execution engine for a session, honouring the user recipe sources of one
 * command invocation: explicitly supplied recipe jars and YAML files plus the YAML files auto-discovered from the
 * config dir.
 *
 * <p>When user sources are present, discovery and execution share one user-aware {@link EnvironmentProvider} — a
 * split environment would list user recipes in the catalog that then fail to resolve at execution time. Lives in
 * core so the CLI commands and the TUI share one implementation of this wiring.
 */
public final class RecipeToolchain {

    /**
     * Discovery and execution wired to the same recipe environment, plus the warnings collected while loading user
     * recipe files (empty when no user sources are involved).
     */
    public record Resolved(
            RecipeDiscoveryService discoveryService, RecipeExecutionEngine engine, List<String> loadWarnings) {

        /** Prints one line per load warning — clear reporting of skipped recipe files without crashing anything. */
        @Requirements({"atunko:CORE_0020.3"})
        public void reportWarnings(PrintWriter err) {
            loadWarnings.forEach(err::println);
            err.flush();
        }
    }

    private RecipeToolchain() {}

    public static Resolved resolve(
            RecipeDiscoveryService sharedDiscovery, RecipeExecutionEngine sharedEngine, List<Path> recipeJars) {
        return resolve(sharedDiscovery, sharedEngine, recipeJars, List.of());
    }

    /**
     * Returns the shared services unchanged when there are no user recipe sources, otherwise a discovery service
     * and engine built around one shared user-aware environment. Auto-discovered config-dir recipe files always
     * take part; explicitly supplied paths are validated eagerly.
     *
     * @throws IllegalArgumentException when a supplied jar or recipe-file path does not exist — scanning silently
     *     past a mistyped path would report an empty catalog with no hint of the cause
     */
    @Requirements({"atunko:CLI_0008.1", "atunko:CORE_0020.1", "atunko:CORE_0020.2"})
    public static Resolved resolve(
            RecipeDiscoveryService sharedDiscovery,
            RecipeExecutionEngine sharedEngine,
            List<Path> recipeJars,
            List<Path> recipeFiles) {
        for (Path jar : recipeJars) {
            if (!Files.isRegularFile(jar)) {
                throw new IllegalArgumentException("Recipe jar not found: " + jar);
            }
        }
        for (Path file : recipeFiles) {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("Recipe file not found: " + file);
            }
        }
        List<Path> files = new ArrayList<>(recipeFiles);
        files.addAll(UserRecipeFiles.discoverDefault());
        if (recipeJars.isEmpty() && files.isEmpty()) {
            return new Resolved(sharedDiscovery, sharedEngine, List.of());
        }
        EnvironmentProvider provider = new EnvironmentProvider(recipeJars, files);
        return new Resolved(
                new RecipeDiscoveryService(provider), new RecipeExecutionEngine(provider), provider.loadWarnings());
    }
}

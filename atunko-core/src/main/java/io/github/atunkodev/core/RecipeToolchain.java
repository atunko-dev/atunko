package io.github.atunkodev.core;

import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.recipe.EnvironmentProvider;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the discovery service and execution engine for a session, honouring user-supplied recipe jars.
 *
 * <p>When jars are supplied, discovery and execution share one jar-aware {@link EnvironmentProvider} — a split
 * environment would list user recipes in the catalog that then fail to resolve at execution time. Lives in core so
 * the CLI commands and the TUI share one implementation of this wiring.
 */
public final class RecipeToolchain {

    /** Discovery and execution wired to the same recipe environment. */
    public record Resolved(RecipeDiscoveryService discoveryService, RecipeExecutionEngine engine) {}

    private RecipeToolchain() {}

    /**
     * Returns the shared services unchanged when no jars are supplied, otherwise a discovery service and engine
     * built around one shared jar-aware environment.
     *
     * @throws IllegalArgumentException when a supplied jar path does not exist — scanning silently past a mistyped
     *     path would report an empty catalog with no hint of the cause
     */
    @Requirements({"atunko:CLI_0008.1"})
    public static Resolved resolve(
            RecipeDiscoveryService sharedDiscovery, RecipeExecutionEngine sharedEngine, List<Path> recipeJars) {
        if (recipeJars.isEmpty()) {
            return new Resolved(sharedDiscovery, sharedEngine);
        }
        for (Path jar : recipeJars) {
            if (!Files.isRegularFile(jar)) {
                throw new IllegalArgumentException("Recipe jar not found: " + jar);
            }
        }
        EnvironmentProvider provider = new EnvironmentProvider(recipeJars);
        return new Resolved(new RecipeDiscoveryService(provider), new RecipeExecutionEngine(provider));
    }
}

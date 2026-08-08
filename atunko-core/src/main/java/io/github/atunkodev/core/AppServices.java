package io.github.atunkodev.core;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SourceCapability;
import io.github.atunkodev.core.recipe.RecipeApplicabilityService;
import io.github.reqstool.annotations.Requirements;
import java.util.Set;

/**
 * Application-scoped execution services set once at startup. Shared by both the TUI and Web UI
 * so that execution logic lives in core and is never duplicated.
 */
public final class AppServices {

    private static volatile RecipeExecutionEngine engine;
    private static volatile ProjectSourceParser sourceParser;
    private static volatile ChangeApplier changeApplier;
    private static final RecipeApplicabilityService APPLICABILITY_SERVICE = new RecipeApplicabilityService();
    private static volatile Set<SourceCapability> sourceCapabilities = Set.of();

    private AppServices() {}

    @Requirements({"atunko:WEB_0001.8"})
    public static void init(
            RecipeExecutionEngine engine, ProjectSourceParser sourceParser, ChangeApplier changeApplier) {
        AppServices.engine = engine;
        AppServices.sourceParser = sourceParser;
        AppServices.changeApplier = changeApplier;
    }

    public static RecipeExecutionEngine getEngine() {
        return engine;
    }

    public static ProjectSourceParser getSourceParser() {
        return sourceParser;
    }

    public static ChangeApplier getChangeApplier() {
        return changeApplier;
    }

    @Requirements({"atunko:CORE_0015"})
    public static RecipeApplicabilityService getRecipeApplicabilityService() {
        return APPLICABILITY_SERVICE;
    }

    /**
     * Capabilities of the source set the session operates on, used to judge recipe applicability. Defaults to the
     * empty set, which is the honest answer before anything has been parsed.
     */
    @Requirements({"atunko:CORE_0015"})
    public static Set<SourceCapability> getSourceCapabilities() {
        return sourceCapabilities;
    }

    public static void setSourceCapabilities(Set<SourceCapability> capabilities) {
        sourceCapabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}

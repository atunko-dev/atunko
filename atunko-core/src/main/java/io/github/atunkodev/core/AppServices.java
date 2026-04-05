package io.github.atunkodev.core;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.reqstool.annotations.Requirements;

/**
 * Application-scoped execution services set once at startup. Shared by both the TUI and Web UI
 * so that execution logic lives in core and is never duplicated.
 */
public final class AppServices {

    private static volatile RecipeExecutionEngine engine;
    private static volatile ProjectSourceParser sourceParser;
    private static volatile ChangeApplier changeApplier;

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
}

package io.github.atunkodev.core.engine;

import io.github.atunkodev.core.project.ProjectEntry;
import io.github.reqstool.annotations.Requirements;
import org.jspecify.annotations.Nullable;

/** The outcome of executing a recipe against a single project. */
@Requirements({"atunko:CORE_0011.1"})
public record ProjectExecutionResult(
        ProjectEntry entry,
        @Nullable ExecutionResult result,
        @Nullable Throwable failure) {

    public boolean succeeded() {
        return failure == null;
    }
}

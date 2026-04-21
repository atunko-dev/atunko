package io.github.atunkodev.core.engine;

import io.github.reqstool.annotations.Requirements;
import java.util.List;

/** Aggregated result of executing a recipe across all projects in a workspace. */
@Requirements({"atunko:CORE_0011"})
public record WorkspaceExecutionResult(List<ProjectExecutionResult> results) {

    public WorkspaceExecutionResult(List<ProjectExecutionResult> results) {
        this.results = List.copyOf(results);
    }

    public long totalChanges() {
        return results.stream()
                .filter(ProjectExecutionResult::succeeded)
                .mapToLong(r -> r.result().changes().size())
                .sum();
    }

    public long failureCount() {
        return results.stream().filter(r -> !r.succeeded()).count();
    }

    public boolean hasFailures() {
        return failureCount() > 0;
    }
}

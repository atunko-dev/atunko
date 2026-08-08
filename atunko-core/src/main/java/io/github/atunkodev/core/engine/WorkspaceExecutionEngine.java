package io.github.atunkodev.core.engine;

import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.Workspace;
import io.github.reqstool.annotations.Requirements;
import java.util.ArrayList;
import java.util.List;
import org.openrewrite.SourceFile;

/** Executes a recipe sequentially across all projects in a {@link Workspace}. */
public class WorkspaceExecutionEngine {

    private final RecipeExecutionEngine engine;
    private final ProjectSourceParser sourceParser;

    public WorkspaceExecutionEngine(RecipeExecutionEngine engine, ProjectSourceParser sourceParser) {
        this.engine = engine;
        this.sourceParser = sourceParser;
    }

    @Requirements({"atunko:CORE_0011", "atunko:CORE_0011.1"})
    public WorkspaceExecutionResult execute(String recipeName, Workspace workspace) {
        return execute(List.of(recipeName), workspace);
    }

    @Requirements({"atunko:CORE_0011", "atunko:CORE_0011.1", "atunko:WEB_0002.1"})
    public WorkspaceExecutionResult execute(List<String> recipeNames, Workspace workspace) {
        List<ProjectExecutionResult> results = new ArrayList<>();
        for (ProjectEntry entry : workspace.projects()) {
            results.add(executeOne(recipeNames, entry));
        }
        return new WorkspaceExecutionResult(results);
    }

    @Requirements({"atunko:CORE_0011.1"})
    private ProjectExecutionResult executeOne(List<String> recipeNames, ProjectEntry entry) {
        try {
            List<SourceFile> sources = sourceParser.parse(entry.info());
            List<FileChange> allChanges = new ArrayList<>();
            for (String recipeName : recipeNames) {
                ExecutionResult r = engine.execute(recipeName, sources);
                allChanges.addAll(r.changes());
            }
            return new ProjectExecutionResult(entry, new ExecutionResult(allChanges), null);
        } catch (Exception e) {
            return new ProjectExecutionResult(entry, null, e);
        }
    }
}

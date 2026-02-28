package io.github.atunkodev.core.engine;

import io.github.reqstool.annotations.Requirements;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
// No public factory for LargeSourceSet exists in OpenRewrite 8.x — InMemoryLargeSourceSet is the only option
import org.openrewrite.internal.InMemoryLargeSourceSet;

public class RecipeExecutionEngine {

    @Requirements({"CORE_0003"})
    public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
        Environment env = Environment.builder().scanRuntimeClasspath().build();
        Recipe recipe = env.activateRecipes(recipeName);
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext());

        List<FileChange> changes = run.getChangeset().getAllResults().stream()
                .filter(result -> result.getBefore() != null || result.getAfter() != null)
                .map(this::toFileChange)
                .toList();

        return new ExecutionResult(changes);
    }

    private FileChange toFileChange(Result result) {
        SourceFile before = result.getBefore();
        SourceFile after = result.getAfter();
        return new FileChange(
                after != null ? after.getSourcePath() : before.getSourcePath(),
                before != null ? before.printAll() : null,
                after != null ? after.printAll() : null);
    }
}

package io.github.atunko.core.engine;

import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;

public class RecipeExecutionEngine {

    @Requirements({"CORE_0003"})
    public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
        Environment env = Environment.builder().scanRuntimeClasspath().build();
        Recipe recipe = env.activateRecipes(recipeName);
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext());

        List<FileChange> changes = run.getChangeset().getAllResults().stream()
                .filter(result -> result.getBefore() != null && result.getAfter() != null)
                .map(this::toFileChange)
                .collect(Collectors.toList());

        return new ExecutionResult(changes);
    }

    private FileChange toFileChange(Result result) {
        return new FileChange(
                result.getAfter().getSourcePath(),
                result.getBefore().printAll(),
                result.getAfter().printAll());
    }
}

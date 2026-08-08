package io.github.atunkodev.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.Workspace;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

@SVCs({"atunko:SVC_CORE_0011"})
class WorkspaceExecutionEngineTest {

    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";

    private static ProjectEntry entry(String name) {
        return new ProjectEntry(Path.of("/projects/" + name), new ProjectInfo(List.of(), List.of()));
    }

    /** Stub parser that always returns an empty source list. */
    private static ProjectSourceParser emptyParser() {
        return new ProjectSourceParser() {
            @Override
            public List<SourceFile> parse(ProjectInfo info) {
                return List.of();
            }
        };
    }

    /** Stub parser that throws for a specific ProjectInfo instance. */
    private static ProjectSourceParser failingParser(ProjectInfo failOn) {
        return new ProjectSourceParser() {
            @Override
            public List<SourceFile> parse(ProjectInfo info) {
                if (info == failOn) {
                    throw new RuntimeException("boom");
                }
                return List.of();
            }
        };
    }

    /** Stub engine that returns a fixed result. */
    private static RecipeExecutionEngine stubEngine(List<FileChange> changes) {
        return new RecipeExecutionEngine() {
            @Override
            public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
                return new ExecutionResult(changes);
            }
        };
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0011"})
    void returnsResultForEveryProject() {
        Workspace workspace =
                new Workspace(Path.of("/projects"), List.of(entry("alpha"), entry("beta"), entry("gamma")));

        WorkspaceExecutionResult result =
                new WorkspaceExecutionEngine(stubEngine(List.of()), emptyParser()).execute(RECIPE, workspace);

        assertThat(result.results()).hasSize(3);
        assertThat(result.results()).allMatch(ProjectExecutionResult::succeeded);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0011.1"})
    void continuesAfterProjectFailure() {
        ProjectEntry failing = entry("failing");
        ProjectEntry ok = entry("ok");

        WorkspaceExecutionEngine engine =
                new WorkspaceExecutionEngine(stubEngine(List.of()), failingParser(failing.info()));

        Workspace workspace = new Workspace(Path.of("/projects"), List.of(failing, ok));
        WorkspaceExecutionResult result = engine.execute(RECIPE, workspace);

        assertThat(result.results()).hasSize(2);
        assertThat(result.results().get(0).succeeded()).isFalse();
        assertThat(result.results().get(0).failure()).hasMessage("boom");
        assertThat(result.results().get(1).succeeded()).isTrue();
        assertThat(result.hasFailures()).isTrue();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0011"})
    void aggregatesTotalChanges() {
        FileChange change = new FileChange(Path.of("Foo.java"), null, "after");
        // Two projects: first gets 1 change, second gets 2
        RecipeExecutionEngine engine = new RecipeExecutionEngine() {
            int call = 0;

            @Override
            public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
                call++;
                return new ExecutionResult(call == 1 ? List.of(change) : List.of(change, change));
            }
        };

        Workspace workspace = new Workspace(Path.of("/projects"), List.of(entry("a"), entry("b")));
        WorkspaceExecutionResult result =
                new WorkspaceExecutionEngine(engine, emptyParser()).execute(RECIPE, workspace);

        assertThat(result.totalChanges()).isEqualTo(3);
    }
}

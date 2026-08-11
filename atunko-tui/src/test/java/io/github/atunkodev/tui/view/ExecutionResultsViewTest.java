package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.Screen;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_TUI_0002.4"})
class ExecutionResultsViewTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java"));

    @Test
    @SVCs({"atunko:SVC_TUI_0002.4"})
    void renderReturnsNonNullElementForWorkspaceResult(@TempDir Path workspaceDir, @TempDir Path projectA) {
        ProjectInfo info = new ProjectInfo(List.of(), List.of());
        ProjectEntry entryA = new ProjectEntry(projectA, info);
        List<ProjectEntry> entries = List.of(entryA);

        WorkspaceExecutionResult fakeResult = new WorkspaceExecutionResult(List.of(new ProjectExecutionResult(
                entryA, new ExecutionResult(List.of(new FileChange(Path.of("Foo.java"), "a", "b"))), null)));

        WorkspaceExecutionEngine stubEngine = new WorkspaceExecutionEngine(null, null) {
            @Override
            public WorkspaceExecutionResult execute(List<String> recipeNames, Workspace workspace) {
                return fakeResult;
            }
        };

        TuiController controller =
                new TuiController(List.of(ALPHA), null, null, null, null, stubEngine, entries, workspaceDir);
        controller.toggleSelection();
        controller.openConfirmRun();
        controller.runSelectedRecipes(false);

        assertThat(controller.currentScreen()).isEqualTo(Screen.WORKSPACE_RESULTS);
        assertThat(controller.lastWorkspaceResult()).isNotNull();
        assertThat(controller.lastWorkspaceResult().results()).hasSize(1);
        assertThat(controller.lastWorkspaceResult().results().getFirst().succeeded())
                .isTrue();

        Element rendered = io.github.atunkodev.tui.shell.TuiShell.render(new ExecutionResultsView(), controller);
        assertThat(rendered).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0002.4"})
    void renderShowsFailureInWorkspaceResult(@TempDir Path workspaceDir, @TempDir Path projectA) {
        ProjectInfo info = new ProjectInfo(List.of(), List.of());
        ProjectEntry entryA = new ProjectEntry(projectA, info);
        List<ProjectEntry> entries = List.of(entryA);

        WorkspaceExecutionResult fakeResult = new WorkspaceExecutionResult(
                List.of(new ProjectExecutionResult(entryA, null, new RuntimeException("build failure"))));

        WorkspaceExecutionEngine stubEngine = new WorkspaceExecutionEngine(null, null) {
            @Override
            public WorkspaceExecutionResult execute(List<String> recipeNames, Workspace workspace) {
                return fakeResult;
            }
        };

        TuiController controller =
                new TuiController(List.of(ALPHA), null, null, null, null, stubEngine, entries, workspaceDir);
        controller.toggleSelection();
        controller.openConfirmRun();
        controller.runSelectedRecipes(false);

        assertThat(controller.lastWorkspaceResult().hasFailures()).isTrue();
        assertThat(io.github.atunkodev.tui.shell.TuiShell.render(new ExecutionResultsView(), controller))
                .isNotNull();
    }
}

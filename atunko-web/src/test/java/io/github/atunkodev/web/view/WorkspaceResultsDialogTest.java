package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkspaceResultsDialogTest {

    private static ProjectEntry entry(String name) {
        return new ProjectEntry(Path.of("/ws/" + name), new ProjectInfo(List.of(), List.of()));
    }

    private static ProjectExecutionResult success(String name, int changes) {
        List<FileChange> fileChanges = new ArrayList<>();
        for (int i = 0; i < changes; i++) {
            fileChanges.add(new FileChange(Path.of("File" + i + ".java"), "old", "new"));
        }
        return new ProjectExecutionResult(entry(name), new ExecutionResult(fileChanges), null);
    }

    private static ProjectExecutionResult failure(String name, String message) {
        return new ProjectExecutionResult(entry(name), null, new RuntimeException(message));
    }

    @BeforeEach
    void setUp() {
        AppServices.init(null, null, null);
        SessionHolder.init(List.of(new ProjectEntry(Path.of("."), null)));
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void dialogTitleIncludesProjectCountForDryRun() {
        WorkspaceExecutionResult result =
                new WorkspaceExecutionResult(List.of(success("alpha", 2), success("beta", 0)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, true);

        assertThat(dialog.getHeaderTitle()).contains("Dry Run");
        assertThat(dialog.getHeaderTitle()).contains("2 project(s)");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void dialogTitleIncludesProjectCountForExecution() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 1)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        assertThat(dialog.getHeaderTitle()).contains("Execution");
        assertThat(dialog.getHeaderTitle()).contains("1 project(s)");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void resultsGridContainsAllProjectRows() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(
                List.of(success("alpha", 1), success("beta", 3), failure("gamma", "build failed")));

        assertThat(result.results()).hasSize(3);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void resultsGridHasExpectedColumnHeaders() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 1)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        @SuppressWarnings("unchecked")
        List<String> headers = ((Grid<ProjectExecutionResult>) _get(dialog, Grid.class))
                .getColumns().stream().map(Grid.Column::getHeaderText).toList();
        assertThat(headers).containsExactly("Project", "Changes", "Status", "Details");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2", "atunko:SVC_WEB_0002.3"})
    void successfulProjectWithChangesShowsViewDiffButton() {
        ProjectExecutionResult pr = success("alpha", 2);
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Button.class);
        assertThat(((Button) cell).getText()).isEqualTo("View Diff");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.3"})
    void eachProjectWithChangesHasItsOwnViewDiffButton() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(
                List.of(success("alpha", 3), success("beta", 1), failure("gamma", "error"), success("delta", 0)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        // Projects with changes each get a scoped View Diff button; no changes or failures get different cells
        Component alphaCell =
                WorkspaceResultsDialog.buildDetailsCell(result.results().get(0), false);
        Component betaCell =
                WorkspaceResultsDialog.buildDetailsCell(result.results().get(1), false);
        Component gammaCell =
                WorkspaceResultsDialog.buildDetailsCell(result.results().get(2), false);
        Component deltaCell =
                WorkspaceResultsDialog.buildDetailsCell(result.results().get(3), false);

        assertThat(alphaCell).isInstanceOf(Button.class);
        assertThat(betaCell).isInstanceOf(Button.class);
        assertThat(gammaCell).isNotInstanceOf(Button.class);
        assertThat(deltaCell).isNotInstanceOf(Button.class);
        assertThat(dialog).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void successfulProjectWithNoChangesShowsNoChangesSpan() {
        ProjectExecutionResult pr = success("alpha", 0);
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Span.class);
        assertThat(((Span) cell).getText()).isEqualTo("No changes");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void failedProjectShowsErrorSpan() {
        ProjectExecutionResult pr = failure("alpha", "compilation error");
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Span.class);
        assertThat(((Span) cell).getText()).isEqualTo("compilation error");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void failedProjectWithNullMessageShowsExceptionClassName() {
        ProjectExecutionResult pr = new ProjectExecutionResult(entry("alpha"), null, new NullPointerException());
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Span.class);
        assertThat(((Span) cell).getText()).isEqualTo("NullPointerException");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void failedProjectWithLongMessageIsTruncated() {
        String longMsg = "x".repeat(200);
        ProjectExecutionResult pr = failure("alpha", longMsg);
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Span.class);
        assertThat(((Span) cell).getText()).hasSize(121);
        assertThat(((Span) cell).getText()).endsWith("…");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void closeButtonIsPresentInFooter() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 0)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);
        dialog.open();

        assertThat(_get(dialog, Button.class, spec -> spec.withText("Close"))).isNotNull();
    }
}

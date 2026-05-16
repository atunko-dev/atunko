package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkspaceResultsDialogTest {

    private static ProjectEntry entry(String name) {
        return new ProjectEntry(Path.of("/ws/" + name), new ProjectInfo(List.of(), List.of()));
    }

    private static ProjectExecutionResult success(String name, int changes) {
        List<FileChange> fileChanges = new java.util.ArrayList<>();
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
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        assertThat(dialog.resultsGrid.getDataCommunicator().getItemCount()).isEqualTo(3);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void resultsGridHasFourColumns() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 1)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        // Project, Changes, Status, Details
        assertThat(dialog.resultsGrid.getColumns()).hasSize(4);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void resultsGridColumnHeadersMatchSpec() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 1)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);

        List<String> headers = dialog.resultsGrid.getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();
        assertThat(headers).containsExactly("Project", "Changes", "Status", "Details");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.2"})
    void successfulProjectWithChangesShowsViewDiffButton() {
        ProjectExecutionResult pr = success("alpha", 2);
        Component cell = WorkspaceResultsDialog.buildDetailsCell(pr, false);

        assertThat(cell).isInstanceOf(Button.class);
        assertThat(((Button) cell).getText()).isEqualTo("View Diff");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0002.3"})
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
    void closeButtonIsPresentInFooter() {
        WorkspaceExecutionResult result = new WorkspaceExecutionResult(List.of(success("alpha", 0)));
        WorkspaceResultsDialog dialog = new WorkspaceResultsDialog(result, false);
        dialog.open();

        assertThat(_get(dialog, Button.class, spec -> spec.withText("Close"))).isNotNull();
    }
}

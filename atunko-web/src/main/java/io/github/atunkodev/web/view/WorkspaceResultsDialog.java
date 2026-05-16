package io.github.atunkodev.web.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.reqstool.annotations.Requirements;

@Requirements({"atunko:WEB_0002.2", "atunko:WEB_0002.3"})
public class WorkspaceResultsDialog extends Dialog {

    final Grid<ProjectExecutionResult> resultsGrid = new Grid<>();

    public WorkspaceResultsDialog(WorkspaceExecutionResult result, boolean dryRun) {
        setHeaderTitle((dryRun ? "Dry Run" : "Execution")
                + " Results — "
                + result.results().size()
                + " project(s)");
        setWidth("800px");

        Grid<ProjectExecutionResult> grid = resultsGrid;
        grid.setItems(result.results());
        grid.addColumn(pr -> pr.entry().projectDir().getFileName().toString())
                .setHeader("Project")
                .setAutoWidth(true);
        grid.addColumn(pr ->
                        pr.succeeded() ? String.valueOf(pr.result().changes().size()) : "-")
                .setHeader("Changes")
                .setAutoWidth(true);
        grid.addColumn(pr -> pr.succeeded() ? "PASS" : "FAIL")
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addComponentColumn(pr -> buildDetailsCell(pr, dryRun)).setHeader("Details");
        grid.setAllRowsVisible(true);
        add(grid);

        Button closeButton = new Button("Close", VaadinIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        getFooter().add(closeButton);
    }

    static com.vaadin.flow.component.Component buildDetailsCell(ProjectExecutionResult pr, boolean dryRun) {
        if (pr.succeeded() && !pr.result().changes().isEmpty()) {
            Button viewButton =
                    new Button("View Diff", VaadinIcon.CODE.create(), e -> new DiffDialog(pr.result(), dryRun).open());
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return viewButton;
        } else if (pr.succeeded()) {
            return new Span("No changes");
        } else {
            Throwable f = pr.failure();
            return new Span(
                    f.getMessage() != null ? f.getMessage() : f.getClass().getSimpleName());
        }
    }
}

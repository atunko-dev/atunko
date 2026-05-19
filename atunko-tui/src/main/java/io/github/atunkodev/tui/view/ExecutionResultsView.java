package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.8", "atunko:TUI_0001.9", "atunko:TUI_0002.4"})
public final class ExecutionResultsView {

    private ExecutionResultsView() {}

    public static Element render(TuiController controller) {
        WorkspaceExecutionResult wsResult = controller.lastWorkspaceResult();
        if (wsResult != null) {
            return renderWorkspaceResult(controller, wsResult);
        }
        String title = controller.lastRunWasDryRun() ? "Dry-Run Preview" : "Execution Results";
        return controller
                .executionResult()
                .map(result -> renderResult(controller, title, result))
                .orElse(text("No results"));
    }

    @Requirements({"atunko:TUI_0002.4"})
    private static Element renderWorkspaceResult(TuiController controller, WorkspaceExecutionResult wsResult) {
        String title = controller.lastRunWasDryRun() ? "Dry-Run Preview" : "Workspace Results";
        var titleElement = controller.lastRunWasDryRun()
                ? text(" " + title + " ").addClass("screen-title", "dryrun-mode")
                : text(" " + title + " ").addClass("screen-title", "success-mode");

        long total = wsResult.totalChanges();
        long failures = wsResult.failureCount();
        String summary = total + " change(s)";
        if (failures > 0) {
            summary += ", " + failures + " failure(s)";
        }

        List<String> rows =
                wsResult.results().stream().map(r -> formatProjectRow(r)).toList();

        return column(dock().top(
                                row(titleElement, spacer(), text(summary + " ").addClass("coverage-indicator")),
                                Constraint.length(1))
                        .center(list(rows)
                                .title("Project Results")
                                .addClass("panel")
                                .autoScroll())
                        .bottom(text(" Esc/q:back").addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("workspace-results")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                        controller.goBack();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private static String formatProjectRow(ProjectExecutionResult r) {
        String projectName = r.entry().projectDir().getFileName().toString();
        if (r.succeeded()) {
            int changes = r.result() != null ? r.result().changes().size() : 0;
            return String.format("%-40s  %4d change(s)  PASS", projectName, changes);
        }
        return String.format("%-40s     -           FAIL", projectName);
    }

    private static Element renderResult(TuiController controller, String title, ExecutionResult result) {
        List<FileChange> changes = result.changes();
        List<String> items = changes.stream().map(c -> c.path().toString()).toList();
        boolean hasChanges = !changes.isEmpty();

        String summary = changes.size() + " file(s) " + (controller.lastRunWasDryRun() ? "would change" : "changed");

        var titleElement = controller.lastRunWasDryRun()
                ? text(" " + title + " ").addClass("screen-title", "dryrun-mode")
                : text(" " + title + " ").addClass("screen-title", "success-mode");

        String footer = hasChanges ? " ↑↓/jk:navigate  Enter:diff  Esc/q:back" : " Esc/q:back";

        return column(dock().top(
                                row(titleElement, spacer(), text(summary + " ").addClass("coverage-indicator")),
                                Constraint.length(1))
                        .center(list(items)
                                .selected(controller.selectedFileIndex())
                                .title("Changed Files")
                                .addClass("panel")
                                .autoScroll())
                        .bottom(text(footer).addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("execution-results")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, event));
    }

    private static EventResult handleKeyEvent(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        boolean hasChanges =
                controller.executionResult().map(r -> !r.changes().isEmpty()).orElse(false);
        if (hasChanges) {
            if (event.isDown() || event.isChar('j')) {
                controller.moveFileDown();
                return EventResult.HANDLED;
            }
            if (event.isUp() || event.isChar('k')) {
                controller.moveFileUp();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                controller.openFileDiff();
                return EventResult.HANDLED;
            }
        }
        if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.goBack();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}

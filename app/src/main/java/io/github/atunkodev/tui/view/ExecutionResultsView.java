package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"CLI_0001.8", "CLI_0001.9"})
public final class ExecutionResultsView {

    private ExecutionResultsView() {}

    public static Element render(TuiController controller) {
        String title = controller.lastRunWasDryRun() ? "Dry-Run Preview" : "Execution Results";

        return controller
                .executionResult()
                .map(result -> renderResult(controller, title, result))
                .orElse(text("No results"));
    }

    private static Element renderResult(TuiController controller, String title, ExecutionResult result) {
        List<FileChange> changes = result.changes();
        List<String> items = changes.stream().map(c -> c.path().toString()).toList();

        String summary = changes.size() + " file(s) " + (controller.lastRunWasDryRun() ? "would change" : "changed");

        var titleElement = controller.lastRunWasDryRun()
                ? text(" " + title).bold().fg(Color.LIGHT_YELLOW)
                : text(" " + title).bold().fg(Color.LIGHT_GREEN);

        return column(dock().top(row(titleElement, spacer(), text(summary + " ").bold()), Constraint.length(1))
                        .center(list(items).title("Changed Files").rounded().autoScroll())
                        .bottom(text(" Esc/q:back").dim(), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("execution-results")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                        controller.goBack();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }
}

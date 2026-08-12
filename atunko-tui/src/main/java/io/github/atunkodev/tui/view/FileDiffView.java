package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.markupTextArea;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.20"})
public final class FileDiffView implements TuiView {

    @Override
    public String id() {
        return "file-diff";
    }

    @Override
    public String title(TuiController controller) {
        return "File Diff";
    }

    @Override
    public String status(TuiController controller) {
        return currentChange(controller)
                .map(change -> (controller.lastRunWasDryRun() ? "dry run | " : "") + change.path())
                .orElse("no file selected");
    }

    @Override
    public List<KeyHint> keyHints(TuiController controller) {
        return AtunkoBindings.hintsFor(AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return AtunkoBindings.helpSections(AtunkoBindings.BACK);
    }

    @Override
    public EventResult handleKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.returnFromFileDiff();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static java.util.Optional<FileChange> currentChange(TuiController controller) {
        return controller.executionResult().flatMap(result -> {
            List<FileChange> changes = result.changes();
            int idx = controller.selectedFileIndex();
            return changes.isEmpty() || idx >= changes.size()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(changes.get(idx));
        });
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        return currentChange(controller)
                .<StyledElement<?>>map(FileDiffView::renderDiff)
                .orElseGet(() -> column(text("No file selected")));
    }

    private static StyledElement<?> renderDiff(FileChange change) {
        String before = change.before() != null ? change.before() : "<empty>";
        String after = change.after() != null ? change.after() : "<empty>";

        return row(
                markupTextArea(before)
                        .title("Before")
                        .showLineNumbers()
                        .scrollbar()
                        .wrapWord()
                        .constraint(Constraint.fill()),
                markupTextArea(after)
                        .title("After")
                        .showLineNumbers()
                        .scrollbar()
                        .wrapWord()
                        .constraint(Constraint.fill()));
    }
}

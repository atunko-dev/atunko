package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Requirements({"atunko:TUI_0001.10"})
public final class LoadConfigView {

    private static final Logger LOG = Logger.getLogger(LoadConfigView.class.getName());

    private LoadConfigView() {}

    public static Element render(TuiController controller) {
        List<Path> configFiles = controller.configFiles();
        int highlightIndex = controller.loadConfigHighlightIndex();

        Element centerContent;
        if (configFiles.isEmpty()) {
            centerContent = column(
                    text(""),
                    text(" No saved configurations found.").addClass("unselected"),
                    text(" Save one first with S in the browser.").addClass("unselected"));
        } else {
            var listColumn = column();
            for (int i = 0; i < configFiles.size(); i++) {
                String filename = configFiles.get(i).getFileName().toString();
                if (i == highlightIndex) {
                    listColumn.add(text(" > " + filename).addClass("highlighted"));
                } else {
                    listColumn.add(text("   " + filename));
                }
            }
            centerContent = listColumn;
        }

        String countText = configFiles.isEmpty() ? "" : configFiles.size() + " configs found";
        Element header = row(text(" Load Config ").addClass("screen-title"), spacer(), text(countText));

        Element statusBar = text(" j/k:navigate  Enter:load  Esc/q:back").addClass("status-bar");

        return column(dock().top(header, Constraint.length(1))
                        .center(centerContent)
                        .bottom(statusBar, Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("load-config")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, event))
                .onMouseEvent(event -> handleMouseEvent(controller, event));
    }

    private static EventResult handleMouseEvent(TuiController controller, MouseEvent event) {
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            controller.moveLoadConfigUp();
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            controller.moveLoadConfigDown();
            return EventResult.HANDLED;
        }
        if (event.isPress() && event.isLeftButton()) {
            int idx = controller.mouseRowToIndex(
                    event.y(), 1, controller.configFiles().size());
            if (idx >= 0) {
                controller.setLoadConfigHighlightIndex(idx);
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleKeyEvent(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isDown()) {
            controller.moveLoadConfigDown();
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
            controller.moveLoadConfigUp();
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            try {
                controller.confirmLoadConfig();
            } catch (java.io.IOException e) {
                LOG.log(Level.WARNING, "Failed to load run config", e);
            }
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE || event.isChar('q')) {
            controller.goBack();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}

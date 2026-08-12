package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Requirements({"atunko:TUI_0001.10"})
public final class LoadConfigView implements TuiView {

    private static final Logger LOG = Logger.getLogger(LoadConfigView.class.getName());

    @Override
    public String id() {
        return "load-config";
    }

    @Override
    public String title(TuiController controller) {
        return "Load Config";
    }

    @Override
    public String status(TuiController controller) {
        int count = controller.configFiles().size();
        return count == 0 ? "no saved configurations" : count + " configs found";
    }

    @Override
    public List<KeyHint> keyHints(TuiController controller) {
        return AtunkoBindings.hintsFor(AtunkoBindings.MOVE, AtunkoBindings.LOAD_CONFIG, AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return AtunkoBindings.helpSections(AtunkoBindings.MOVE, AtunkoBindings.LOAD_CONFIG, AtunkoBindings.BACK);
    }

    @Override
    public EventResult handleKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        return handleKeyEvent(controller, event);
    }

    @Override
    public EventResult handleMouse(TuiController controller, MouseEvent event) {
        return handleMouseEvent(controller, event);
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        List<Path> configFiles = controller.configFiles();
        int highlightIndex = controller.loadConfigHighlightIndex();

        if (configFiles.isEmpty()) {
            return column(
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
            return listColumn;
        }
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

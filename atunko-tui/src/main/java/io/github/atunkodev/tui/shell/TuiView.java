package io.github.atunkodev.tui.shell;

import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.view.HelpOverlay;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

/**
 * What every screen must declare about itself.
 *
 * <p>A screen supplies its content and four descriptions of itself; {@link TuiShell} supplies the frame around them.
 * The point of the contract is that {@link #keyHints} and {@link #helpSections} are <em>required</em> — a screen
 * cannot exist without saying what its keys do, which is what stops the footer, the help screen and the docs
 * drifting apart from the behaviour. Maven Pilot's {@code ToolPanel} and Camel's {@code MonitorTab} both make the
 * same members mandatory for the same reason.
 */
@Requirements({"atunko:TUI_0009"})
public interface TuiView {

    /**
     * Shown in the header. Short — it sits beside the tab bar.
     *
     * <p>Takes the controller because several screens title themselves from state: the results screen reads
     * "Execution Failed" or "Dry-Run Preview" depending on the run, and the browser switches to "SEARCH".
     */
    String title(TuiController controller);

    /**
     * Extra style classes for the title, for screens that highlight a mode — the browser turns the title yellow while
     * searching. The shell always adds {@code screen-title}; these come on top of it.
     */
    default List<String> titleClasses(TuiController controller) {
        return List.of();
    }

    /** The state line of the footer: counts, active filters, current mode. Never key hints — those are separate. */
    String status(TuiController controller);

    /**
     * Key hints for the footer, in the order they should read. Rendered as emphasised key + dim label pairs, so the
     * key is the thing the eye finds.
     */
    List<KeyHint> keyHints(TuiController controller);

    /** Sections shown when help is opened from this screen. */
    List<HelpOverlay.Section> helpSections();

    /**
     * The screen's own content, without header, footer or details pane.
     *
     * <p>Typed as {@link StyledElement} rather than {@code Element} so the shell can set the layout constraint —
     * geometry is the shell's business, and {@code Element} exposes only a constraint getter.
     */
    StyledElement<?> renderContent(TuiController controller);

    /**
     * Optional right-hand/bottom details pane. Returning {@code null} — the default — means this screen has no
     * details region and the shell gives its space back to the content.
     */
    default StyledElement<?> renderDetails(TuiController controller) {
        return null;
    }

    /**
     * Extra header content for this screen — a search box, a filter indicator. The shell places it beside the
     * title, so a screen can contribute to the header without owning its geometry.
     */
    default StyledElement<?> renderHeaderExtras(TuiController controller) {
        return null;
    }

    /**
     * Stable id for the screen's root element — used for focus traversal and by the headless Pilot tests.
     */
    String id();

    /**
     * Handles a key press for this screen.
     *
     * <p>On the contract because the shell owns the root element, and TamboUI attaches handlers to elements: a view
     * that built its own root could attach its own handler, a view that only supplies content cannot. Both
     * reference implementations put key and mouse handling on the same contract for the same reason.
     */
    default EventResult handleKey(TuiController controller, KeyEvent event) {
        return EventResult.UNHANDLED;
    }

    default EventResult handleMouse(TuiController controller, MouseEvent event) {
        return EventResult.UNHANDLED;
    }
}

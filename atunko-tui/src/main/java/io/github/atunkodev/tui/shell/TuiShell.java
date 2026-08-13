package io.github.atunkodev.tui.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.elements.Row;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

/**
 * The one frame every screen is drawn inside.
 *
 * <p>Before this existed each view built its own {@code dock()}, and the header was 3 rows in the browser, 1 row in
 * most screens and computed in the detail view — so the frame jumped on every navigation. Geometry lives here now,
 * which is the only way "every screen looks the same" can be true by construction rather than by remembering.
 *
 * <pre>
 *   header   length(3)      title + tab bar
 *   content  fill()         the view's own element
 *   details  percentage(30) or length(0) when absent or an overlay is open
 *   footer   length(2)      status row, key-hint row
 * </pre>
 *
 * <p>Proportions follow Maven Pilot's {@code ToolPanel}, minus its blank spacer row.
 */
@Requirements({"atunko:TUI_0009", "atunko:TUI_0009.1"})
public final class TuiShell {

    /** Header height, fixed so navigation never shifts the content region. */
    public static final int HEADER_HEIGHT = 3;

    /** Footer height: one state row, one key-hint row. */
    public static final int FOOTER_HEIGHT = 2;

    /** Share of the frame given to a details pane when the screen has one. */
    public static final int DETAILS_PERCENT = 30;

    /** Focusable region ids, so Tab has something to traverse and tests can name what holds focus. */
    public static final String CONTENT_REGION = "content";

    public static final String DETAILS_REGION = "details";

    private TuiShell() {}

    /**
     * Renders {@code view} inside the shared frame.
     *
     * @param overlay drawn over the content when non-{@code null}; the details pane collapses so the overlay gets
     *     the full region, which is how Pilot avoids a second layout path for overlays
     * @param overlayHints replaces the view's own hints while an overlay is open, so the footer describes the mode
     *     the user is actually in rather than the one underneath
     */
    @Requirements({"atunko:TUI_0009.1", "atunko:TUI_0009.4"})
    public static Element render(TuiView view, TuiController controller, Element overlay, List<KeyHint> overlayHints) {

        Element body = overlay != null ? overlay : contentWithDetails(view, controller);
        List<KeyHint> hints = overlay != null ? overlayHints : view.keyHints(controller);

        return column(dock().top(renderHeader(view, controller), Constraint.length(HEADER_HEIGHT))
                        .center(body)
                        .bottom(renderFooter(view, controller, hints), Constraint.length(FOOTER_HEIGHT))
                        .constraint(Constraint.fill()))
                .id(view.id())
                .addClass("app")
                .focusable()
                .onKeyEvent(event -> dispatchKey(view, controller, event))
                .onMouseEvent(event -> view.handleMouse(controller, event));
    }

    /** Convenience for a screen with no overlay open. */
    public static Element render(TuiView view, TuiController controller) {
        return render(view, controller, null, List.of());
    }

    private static Element contentWithDetails(TuiView view, TuiController controller) {
        StyledElement<?> details = view.renderDetails(controller);
        StyledElement<?> content = view.renderContent(controller);
        if (details == null) {
            return content.constraint(Constraint.fill());
        }
        // The framework owns traversal: Tab is bound to FOCUS_NEXT in the standard binding set and is consumed
        // before an element handler ever sees it, so the shell cannot implement Tab itself. What it must do is
        // put the screen's handler on every focusable region — otherwise keys route to a focused region that has
        // no handler and navigation dies, which is exactly what happened when only the root carried it.
        return row(
                        focusableRegion(content.constraint(Constraint.fill()), CONTENT_REGION, view, controller),
                        focusableRegion(
                                details.constraint(Constraint.percentage(DETAILS_PERCENT)),
                                DETAILS_REGION,
                                view,
                                controller))
                .constraint(Constraint.fill());
    }

    @Requirements({"atunko:TUI_0009.5"})
    private static Element focusableRegion(
            StyledElement<?> element, String region, TuiView view, TuiController controller) {
        return element.id(region)
                .focusable()
                .onKeyEvent(event -> dispatchKey(view, controller, event))
                .onMouseEvent(event -> view.handleMouse(controller, event));
    }

    /**
     * Handles help before the screen sees the key, so {@code ?} works everywhere instead of only on the three
     * screens that happened to implement it.
     *
     * <p>The dismissing key is passed on to the screen unless it is {@code ?} itself: reading help and pressing
     * {@code r} to run should run, not merely close the overlay and discard the press.
     */
    @Requirements({"atunko:TUI_0009.7", "atunko:TUI_0009.4"})
    static EventResult dispatchKey(TuiView view, TuiController controller, KeyEvent event) {
        if (controller.isShowHelp()) {
            controller.toggleHelp();
            return event.isChar('?') ? EventResult.HANDLED : view.handleKey(controller, event);
        }
        if (event.isChar('?')) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        return view.handleKey(controller, event);
    }

    /**
     * Title, tabs and the screen's own control on one row, inside a bordered band.
     *
     * <p>The panel's border plus its single content row is exactly {@link #HEADER_HEIGHT}, so everything in the band
     * lines up by construction. Loose elements in a 3-row strip did not: the title's background painted all three rows
     * while the tab bar sat on the first, and only a bordered input ever filled the band. It also matches the panels
     * below, so header controls must be single-line — see the views' header extras.
     */
    @Requirements({"atunko:TUI_0009.8"})
    private static Element renderHeader(TuiView view, TuiController controller) {
        StyledElement<?> extras = view.renderHeaderExtras(controller);
        StyledElement<?> title = text(" " + view.title(controller) + " ").addClass("screen-title");
        view.titleClasses(controller).forEach(title::addClass);
        Row header = row(title, text("  "));
        header.add(TabBar.render(controller));
        if (extras != null) {
            header.add(extras);
        } else {
            header.add(spacer());
        }
        return panel(header).addClass("panel").constraint(Constraint.length(HEADER_HEIGHT));
    }

    /**
     * Two rows: state, then hints. The split is the point — the old single-row footer interleaved counts and key
     * hints in one sentence, so neither read cleanly.
     */
    @Requirements({"atunko:TUI_0009.2"})
    private static Element renderFooter(TuiView view, TuiController controller, List<KeyHint> hints) {
        return column(text(" " + view.status(controller)).addClass("status-bar"), renderHints(hints));
    }

    /** Key and label styled separately, so the eye finds the key without reading the sentence. */
    @Requirements({"atunko:TUI_0009.2"})
    static Element renderHints(List<KeyHint> hints) {
        Row line = row(text(" "));
        for (KeyHint hint : hints) {
            line.add(text(hint.key()).addClass("hint-key"));
            line.add(text(":" + hint.label() + "  ").addClass("hint-label"));
        }
        line.add(spacer());
        return line.addClass("hint-bar");
    }
}

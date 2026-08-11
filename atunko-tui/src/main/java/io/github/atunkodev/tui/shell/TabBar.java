package io.github.atunkodev.tui.shell;

import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.elements.Row;
import io.github.atunkodev.tui.Screen;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

/**
 * The header tab bar: which screens exist, which one you are on, and how much is on each.
 *
 * <p>atunko previously spent the tab affordance on a <em>sort order</em> ({@code tabs(NAME, TAGS, RECENT)}) while
 * having eight screens and no tab navigation at all — tabs that looked like destinations but were a filter. Sort
 * moved to the status row beside the other filters; tabs now address screens.
 *
 * <p>Labels carry live counts, following Maven Pilot's tab bar, which renders
 * {@code Declared: 42 (3 unused)}. That makes the bar a summary as well as a switcher.
 */
@Requirements({"atunko:TUI_0009.3"})
public final class TabBar {

    /** A destination and how much is on it. */
    public record Tab(Screen screen, String label, int count) {}

    private TabBar() {}

    /** Tabs for the top-level destinations, with counts read from the controller. */
    @Requirements({"atunko:TUI_0009.3"})
    public static List<Tab> tabsFor(TuiController controller) {
        long recipes =
                controller.displayRows().stream().filter(r -> !r.isSubRecipe()).count();
        return List.of(
                new Tab(Screen.BROWSER, "Recipes", (int) recipes),
                new Tab(Screen.TAG_BROWSER, "Tags", controller.allTags().size()),
                new Tab(Screen.CONFIRM_RUN, "Run", controller.selectedRecipes().size()));
    }

    /** Renders the bar, marking the active screen. Inactive tabs are dimmed, as in Pilot. */
    @Requirements({"atunko:TUI_0009.3"})
    public static Row render(TuiController controller) {
        Row bar = row();
        for (Tab tab : tabsFor(controller)) {
            boolean active = tab.screen() == controller.currentScreen();
            String marker = active ? "▸ " : "  ";
            bar.add(text("[" + marker + tab.label() + ": " + tab.count() + "]  ")
                    .addClass(active ? "tab-active" : "tab-inactive"));
        }
        return bar;
    }
}

package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Column;
import java.util.List;

public final class HelpOverlay {

    public record Entry(String key, String description) {}

    public record Section(String title, List<Entry> entries) {}

    private HelpOverlay() {}

    public static final List<Section> BROWSER_HELP = List.of(
            new Section(
                    "Navigation",
                    List.of(
                            new Entry("\u2191\u2193", "Move"),
                            new Entry("\u2192", "Expand"),
                            new Entry("\u2190", "Collapse"))),
            new Section("Selection", List.of(new Entry("Space", "Toggle"), new Entry("a", "All/none"))),
            new Section(
                    "Actions",
                    List.of(
                            new Entry("Enter", "Detail view"),
                            new Entry("r", "Run dialog"),
                            new Entry("t", "Tag browser"),
                            new Entry("s", "Sort order"),
                            new Entry("/", "Search"),
                            new Entry("Esc", "Clear all"),
                            new Entry("q", "Quit"))),
            new Section("Legend", List.of(new Entry("[x]", "Selected"), new Entry("[c]", "Covered by composite"))));

    public static final List<Section> RUN_DIALOG_HELP = List.of(
            new Section(
                    "Navigation",
                    List.of(
                            new Entry("\u2191\u2193", "Move"),
                            new Entry("+/-", "Reorder"),
                            new Entry("\u2192", "Expand"),
                            new Entry("\u2190", "Collapse"))),
            new Section("Selection", List.of(new Entry("Space", "Toggle"), new Entry("a", "All/none"))),
            new Section(
                    "Actions",
                    List.of(
                            new Entry("r", "Run"),
                            new Entry("d", "Dry-run"),
                            new Entry("f", "Flatten"),
                            new Entry("Esc", "Back"))));

    public static final List<Section> DETAIL_HELP = List.of(
            new Section("Actions", List.of(new Entry("Space", "Toggle selection"), new Entry("Esc/q", "Back"))));

    public static Element render(List<Section> sections) {
        Column cols = column();
        for (Section section : sections) {
            cols.add(text(" " + section.title()).bold().fg(Color.LIGHT_CYAN));
            for (Entry entry : section.entries()) {
                cols.add(row(text("  " + padRight(entry.key(), 8)).fg(Color.LIGHT_YELLOW), text(entry.description())));
            }
            cols.add(text(""));
        }
        return panel("Help — press any key to close", cols)
                .rounded()
                .borderColor(Color.LIGHT_CYAN)
                .constraint(Constraint.percentage(60));
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }
}

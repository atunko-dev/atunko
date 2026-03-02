package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

public final class RecipeListRenderer {

    public record RenderOptions(boolean showNumbering, boolean showTags, boolean dimUnselected) {
        public static final RenderOptions BROWSER = new RenderOptions(false, true, false);
        public static final RenderOptions RUN_DIALOG = new RenderOptions(true, false, true);
    }

    private RecipeListRenderer() {}

    @Requirements({"atunko:TUI_0001.16"})
    public static Element renderRecipeList(
            List<DisplayRow> displayRows,
            Set<String> selectedRecipes,
            Set<String> expandedRecipes,
            Set<String> coveredRecipes,
            int highlightedIndex,
            String title,
            RenderOptions options,
            Constraint constraint) {
        var recipeList =
                list().highlightStyle(Style.EMPTY.fg(Color.WHITE).bg(Color.BLUE).bold());

        int parentIndex = 0;
        for (DisplayRow displayRow : displayRows) {
            RecipeInfo r = displayRow.recipe();
            boolean selected = selectedRecipes.contains(r.name());
            boolean covered = coveredRecipes.contains(r.name());
            boolean expanded = expandedRecipes.contains(r.name());
            String check = resolveCheckbox(selected, covered, displayRow.isSubRecipe());
            String indicator = r.isComposite() ? (expanded ? "\u25bc " : "\u25b6 ") : "  ";

            String prefix;
            if (displayRow.isSubRecipe()) {
                String indent = "  ".repeat(displayRow.depth());
                prefix = (options.showNumbering() ? "    " : "") + indent + check + indicator;
            } else {
                parentIndex++;
                prefix = options.showNumbering()
                        ? String.format("%2d. %s%s", parentIndex, check, indicator)
                        : check + indicator;
            }

            var prefixEl = resolvePrefixStyle(prefix, selected, covered, displayRow.isSubRecipe());
            String displayName = cleanDisplayName(r.displayName());
            var nameEl = resolveNameStyle(displayName, selected, covered, options);

            if (options.showTags() && !r.tags().isEmpty() && !displayRow.isSubRecipe()) {
                var tags = text("  " + String.join(", ", r.tags())).dim();
                recipeList.add(row(prefixEl, nameEl, spacer(), tags));
            } else {
                recipeList.add(row(prefixEl, nameEl));
            }
        }

        var result = recipeList
                .selected(highlightedIndex)
                .title(title)
                .rounded()
                .borderColor(Color.LIGHT_CYAN)
                .autoScroll();

        if (constraint != null) {
            return result.constraint(constraint);
        }
        return result;
    }

    private static String resolveCheckbox(boolean selected, boolean covered, boolean isSubRecipe) {
        if (selected) {
            return "[x] ";
        }
        if (covered && isSubRecipe) {
            return "[\u2713] ";
        }
        if (covered) {
            return "[\u2248] ";
        }
        return "[ ] ";
    }

    private static Element resolvePrefixStyle(String prefix, boolean selected, boolean covered, boolean isSubRecipe) {
        if (selected) {
            return text(prefix).fg(Color.LIGHT_GREEN);
        }
        if (covered && isSubRecipe) {
            return text(prefix).fg(Color.indexed(65));
        }
        if (covered) {
            return text(prefix).dim();
        }
        return text(prefix).dim();
    }

    private static Element resolveNameStyle(
            String displayName, boolean selected, boolean covered, RenderOptions options) {
        if (covered && !selected) {
            return text(displayName).dim();
        }
        if (options.dimUnselected() && !selected) {
            return text(displayName).dim();
        }
        return text(displayName);
    }

    public static String cleanDisplayName(String displayName) {
        return displayName.replace("`", "");
    }
}

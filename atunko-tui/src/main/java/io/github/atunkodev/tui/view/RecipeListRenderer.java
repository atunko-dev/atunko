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
import java.util.List;
import java.util.Set;

public final class RecipeListRenderer {

    public record RenderOptions(boolean showNumbering, boolean showTags, boolean dimUnselected) {
        public static final RenderOptions BROWSER = new RenderOptions(false, true, false);
        public static final RenderOptions RUN_DIALOG = new RenderOptions(true, false, true);
    }

    private RecipeListRenderer() {}

    public static Element renderRecipeList(
            List<DisplayRow> displayRows,
            Set<String> selectedRecipes,
            Set<String> expandedRecipes,
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
            boolean expanded = expandedRecipes.contains(r.name());
            String check = selected ? "[x] " : "[ ] ";
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

            var prefixEl =
                    selected ? text(prefix).fg(Color.LIGHT_GREEN) : text(prefix).dim();
            String displayName = cleanDisplayName(r.displayName());
            var nameEl =
                    (options.dimUnselected() && !selected) ? text(displayName).dim() : text(displayName);

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

    public static String cleanDisplayName(String displayName) {
        return displayName.replace("`", "");
    }
}

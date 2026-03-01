package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"CLI_0001.11"})
public final class TagBrowserView {

    private static int tagIndex;

    private TagBrowserView() {}

    public static Element render(TuiController controller, AtunkoTui app) {
        List<String> tags = controller.allTags();

        return dock().top(text(" Tag Browser").bold().cyan(), Constraint.length(1))
                .center(list(tags)
                        .selected(tagIndex)
                        .title("Tags")
                        .rounded()
                        .autoScroll()
                        .focusable()
                        .onKeyEvent(event -> {
                            if (event.isDown()) {
                                tagIndex = Math.min(tagIndex + 1, tags.size() - 1);
                                return EventResult.HANDLED;
                            }
                            if (event.isUp()) {
                                tagIndex = Math.max(tagIndex - 1, 0);
                                return EventResult.HANDLED;
                            }
                            if (event.isConfirm() && !tags.isEmpty()) {
                                controller.filterByTag(tags.get(tagIndex));
                                tagIndex = 0;
                                return EventResult.HANDLED;
                            }
                            if (event.character() == 'q' || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                                controller.goBack();
                                tagIndex = 0;
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        }))
                .bottom(text(" ↑↓:navigate Enter:filter by tag Esc/q:back").dim(), Constraint.length(1));
    }
}

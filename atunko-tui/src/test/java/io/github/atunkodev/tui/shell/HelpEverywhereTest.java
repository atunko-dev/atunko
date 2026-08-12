package io.github.atunkodev.tui.shell;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.view.BrowserView;
import io.github.atunkodev.tui.view.ConfirmRunView;
import io.github.atunkodev.tui.view.DetailView;
import io.github.atunkodev.tui.view.ExecutionResultsView;
import io.github.atunkodev.tui.view.FileDiffView;
import io.github.atunkodev.tui.view.LoadConfigView;
import io.github.atunkodev.tui.view.TagBrowserView;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Help must open on every screen.
 *
 * <p>Before the shell existed it was reachable from three of the eight screens, because each screen had to
 * implement {@code ?} for itself and most did not. {@link TuiShell#dispatchKey} now handles it before the screen
 * sees the key, so the guarantee is structural.
 */
class HelpEverywhereTest {

    private static TuiController controller() {
        return new TuiController(
                List.of(new RecipeInfo("org.example.First", "First", "Does the first thing", Set.of("java"))));
    }

    static Stream<TuiView> everyScreen() {
        return Stream.of(
                new BrowserView(new AtunkoTui(controller())),
                new DetailView(),
                new TagBrowserView(),
                new ConfirmRunView(),
                new ExecutionResultsView(),
                new LoadConfigView(),
                new FileDiffView());
    }

    @ParameterizedTest
    @MethodSource("everyScreen")
    void helpOpensOnEveryScreen(TuiView view) {
        TuiController controller = controller();
        assertThat(controller.isShowHelp()).isFalse();

        EventResult result = TuiShell.dispatchKey(view, controller, KeyEvent.ofChar('?'));

        assertThat(result).as("%s must handle help", view.id()).isEqualTo(EventResult.HANDLED);
        assertThat(controller.isShowHelp()).as("%s must open help", view.id()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("everyScreen")
    void everyScreenSuppliesItsOwnHelpContent(TuiView view) {
        assertThat(view.helpSections())
                .as("%s must describe its own keys", view.id())
                .isNotEmpty();
    }

    /** Plain {@code @Test} so reqstool can attribute the SVC — it cannot recover parameterised method names. */
    @Test
    @SVCs({"atunko:SVC_TUI_0009.7"})
    void helpOpensAndClosesFromAnyScreen() {
        TuiController controller = controller();
        TuiView view = new DetailView();

        TuiShell.dispatchKey(view, controller, KeyEvent.ofChar('?'));
        assertThat(controller.isShowHelp()).isTrue();

        TuiShell.dispatchKey(view, controller, KeyEvent.ofChar('?'));
        assertThat(controller.isShowHelp()).as("? is a toggle").isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void aNonHelpKeyClosesHelpAndStillReachesTheScreen() {
        TuiController controller = controller();
        TuiView view = new BrowserView(new AtunkoTui(controller));

        TuiShell.dispatchKey(view, controller, KeyEvent.ofChar('?'));
        assertThat(controller.isShowHelp()).isTrue();

        TuiShell.dispatchKey(view, controller, KeyEvent.ofChar(' '));

        assertThat(controller.isShowHelp()).as("help closed").isFalse();
        assertThat(controller.selectedRecipes())
                .as("and the key still did its job, rather than being swallowed")
                .isNotEmpty();
    }
}

package io.github.atunkodev.tui.shell;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.view.BrowserView;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TuiShellTest {

    private static TuiController controller() {
        return new TuiController(List.of(
                new RecipeInfo("org.example.First", "First", "Does the first thing", Set.of("java")),
                new RecipeInfo("org.example.Second", "Second", "Does the second thing", Set.of("java", "spring"))));
    }

    /** Every screen migrated onto the contract. Section 8 adds the assertion that this covers every Screen. */
    static Stream<TuiView> migratedViews() {
        return Stream.of(new BrowserView(new AtunkoTui(controller())));
    }

    @ParameterizedTest
    @MethodSource("migratedViews")
    @SVCs({"atunko:SVC_TUI_0009"})
    void everyViewSuppliesTitleStatusHintsAndHelp(TuiView view) {
        TuiController controller = controller();

        assertThat(view.id()).as("id").isNotBlank();
        assertThat(view.title()).as("title").isNotBlank();
        assertThat(view.status(controller)).as("status").isNotBlank();
        assertThat(view.keyHints(controller)).as("key hints").isNotEmpty();
        assertThat(view.helpSections()).as("help sections").isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("migratedViews")
    @SVCs({"atunko:SVC_TUI_0009"})
    void statusCarriesNoKeyHints(TuiView view) {
        // The old footer mixed counts and key hints into one sentence; the split is the point of the two-row footer.
        assertThat(view.status(controller()))
                .as("status row must carry state only — hints belong on their own row")
                .doesNotContain("?:help")
                .doesNotContain("q:quit");
    }

    @ParameterizedTest
    @MethodSource("migratedViews")
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void everyKeyHintNamesAKeyAndAnAction(TuiView view) {
        assertThat(view.keyHints(controller())).allSatisfy(hint -> {
            assertThat(hint.key()).isNotBlank();
            assertThat(hint.label()).isNotBlank();
        });
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.1"})
    void frameHeightsAreConstantsOwnedByTheShell() {
        // Geometry lives in one place; a view cannot choose its own header height any more.
        assertThat(TuiShell.HEADER_HEIGHT).isEqualTo(3);
        assertThat(TuiShell.FOOTER_HEIGHT).isEqualTo(2);
    }
}

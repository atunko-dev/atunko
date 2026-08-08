package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.event.KeyCode;
import io.github.atunkodev.core.project.SourceCapability;
import io.github.reqstool.annotations.SVCs;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification that recipes which cannot act on the parsed source set are badged in the real rendered
 * frames. The default capability set is empty — nothing has been parsed — which is exactly the Stage 1 situation for
 * Maven and Gradle recipes: {@code pom.xml} is parsed as plain XML and Gradle build files are not parsed at all.
 */
@SVCs({"atunko:SVC_TUI_0004"})
class AtunkoTuiApplicabilityPilotTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0004"})
    void browserBadgesMavenAndGradleRecipesButNotJavaRecipes() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(PilotTestSupport.APPLICABILITY_RECIPES)) {
            String screen = tui.screen();

            assertThat(screen).contains("⊘ needs Maven");
            assertThat(screen).contains("⊘ needs Gradle");
            // the java recipe can act on any source set, so its row carries no badge
            String javaRow = rowContaining(screen, "Remove unused imports");
            assertThat(javaRow).doesNotContain("⊘");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0004"})
    void badgesDisappearWhenTheSourceSetProvidesTheCapability() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(PilotTestSupport.APPLICABILITY_RECIPES)) {
            assertThat(tui.screen()).contains("⊘ needs Maven");

            tui.controller().setSourceCapabilities(Set.of(SourceCapability.JAVA, SourceCapability.MAVEN));
            // force a re-render through the live pipeline
            tui.pilot().press(KeyCode.DOWN);

            assertThat(tui.screen()).doesNotContain("⊘ needs Maven");
            assertThat(tui.screen()).contains("⊘ needs Gradle");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0004"})
    void badgingDoesNotChangeSelectionBehaviour() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(PilotTestSupport.APPLICABILITY_RECIPES)) {
            highlight(tui, "org.openrewrite.maven.ChangePropertyValue");
            tui.pilot().press(' ');

            assertThat(tui.controller().selectedRecipes()).containsExactly("org.openrewrite.maven.ChangePropertyValue");
            assertThat(tui.screen()).contains("[x]");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0004.1"})
    void detailViewShowsTheApplicabilityReasonForAnInapplicableRecipe() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(PilotTestSupport.APPLICABILITY_RECIPES)) {
            highlight(tui, "org.openrewrite.maven.ChangePropertyValue");
            tui.pilot().press(KeyCode.ENTER);

            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.DETAIL);
            String screen = tui.screen();
            assertThat(screen).contains("Applicability:");
            assertThat(screen).contains("Requires Maven build model");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0004.1"})
    void detailViewOmitsTheApplicabilityLineForAnApplicableRecipe() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(PilotTestSupport.APPLICABILITY_RECIPES)) {
            highlight(tui, "org.openrewrite.java.RemoveUnusedImports");
            tui.pilot().press(KeyCode.ENTER);

            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.DETAIL);
            assertThat(tui.screen()).contains("org.openrewrite.java.RemoveUnusedImports");
            assertThat(tui.screen()).doesNotContain("Applicability:");
        }
    }

    /** Moves the browser highlight onto the named recipe; the browser sorts by name, so indices are not stable. */
    private static void highlight(PilotTestSupport tui, String recipeName) {
        for (int i = 0; i < PilotTestSupport.APPLICABILITY_RECIPES.size(); i++) {
            if (tui.controller()
                    .highlightedRecipe()
                    .filter(r -> r.name().equals(recipeName))
                    .isPresent()) {
                return;
            }
            tui.pilot().press(KeyCode.DOWN);
        }
        throw new AssertionError("Recipe never became highlighted: " + recipeName);
    }

    /** Returns the single rendered line containing {@code needle}, so per-row assertions stay unambiguous. */
    private static String rowContaining(String screen, String needle) {
        return screen.lines()
                .filter(line -> line.contains(needle))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No rendered line contains: " + needle));
    }
}

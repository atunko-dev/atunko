package io.github.atunkodev.tui.shell;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.event.KeyEvent;
import io.github.atunkodev.tui.shell.AtunkoBindings.Binding;
import io.github.atunkodev.tui.view.HelpOverlay;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AtunkoBindingsTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void everyBindingCarriesAKeyAndADescription() {
        assertThat(AtunkoBindings.all()).isNotEmpty().allSatisfy(binding -> {
            assertThat(binding.action()).as("action").isNotBlank();
            assertThat(binding.hintKey()).as("hint key").isNotBlank();
            assertThat(binding.description()).as("description").isNotBlank();
            assertThat(binding.section()).as("help section").isNotBlank();
        });
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void noTwoBindingsShareATrigger() {
        Map<String, List<Binding>> byTrigger = AtunkoBindings.all().stream()
                .filter(b -> b.trigger() != null)
                .collect(Collectors.groupingBy(b -> b.trigger().describe()));

        assertThat(byTrigger)
                .allSatisfy((trigger, bindings) -> assertThat(bindings)
                        .as("trigger %s is bound to more than one action", trigger)
                        .hasSize(1));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void noTwoBindingsShareAnAction() {
        assertThat(AtunkoBindings.all().stream().map(Binding::action).distinct().count())
                .isEqualTo(AtunkoBindings.all().size());
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void navigationIsNotBoundToLetterKeys() {
        Bindings bindings = AtunkoBindings.bindings();

        // BindingSets.standard() is documented as carrying no vim or emacs bindings and no letter key bound to
        // navigation. This is the owner's constraint, so assert it rather than trusting the base set stays that way.
        assertThat(bindings.actionFor(keyEvent('j')))
                .as("j must not move the cursor")
                .isNotEqualTo(java.util.Optional.of(Actions.MOVE_DOWN));
        assertThat(bindings.actionFor(keyEvent('k')))
                .as("k must not move the cursor")
                .isNotEqualTo(java.util.Optional.of(Actions.MOVE_UP));
        assertThat(bindings.actionFor(keyEvent('h'))).isNotEqualTo(java.util.Optional.of(Actions.MOVE_LEFT));
        assertThat(bindings.actionFor(keyEvent('l'))).isNotEqualTo(java.util.Optional.of(Actions.MOVE_RIGHT));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void everyDeclaredActionResolvesFromItsTrigger() {
        Bindings bindings = AtunkoBindings.bindings();

        for (Binding binding : AtunkoBindings.all()) {
            if (binding.trigger() == null) {
                continue; // framework-provided; described here, bound by the standard set
            }
            assertThat(bindings.triggersFor(binding.action()))
                    .as("action %s must be reachable from its declared trigger", binding.action())
                    .isNotEmpty();
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.6", "atunko:SVC_TUI_0009.2"})
    void hintsAndHelpComeFromTheSameDeclaration() {
        List<KeyHint> hints = AtunkoBindings.hintsFor(AtunkoBindings.OPEN_RUN, AtunkoBindings.HELP);
        List<HelpOverlay.Section> help = AtunkoBindings.helpSections(AtunkoBindings.OPEN_RUN, AtunkoBindings.HELP);

        Map<String, String> hintByKey = hints.stream().collect(Collectors.toMap(KeyHint::key, KeyHint::label));
        Map<String, String> helpByKey = help.stream()
                .flatMap(s -> s.entries().stream())
                .collect(Collectors.toMap(HelpOverlay.Entry::key, HelpOverlay.Entry::description));

        assertThat(hintByKey.keySet())
                .as("the footer and the help screen must describe the same keys")
                .isEqualTo(helpByKey.keySet());
        assertThat(hintByKey.keySet()).containsExactlyInAnyOrder("r", "?");

        // Same source, so a description change moves both — case is the only difference, by design.
        hintByKey.forEach((key, label) -> assertThat(helpByKey.get(key).toLowerCase(java.util.Locale.ROOT))
                .isEqualTo(label));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.7"})
    void helpSectionsPreserveDeclarationGrouping() {
        List<HelpOverlay.Section> sections =
                AtunkoBindings.helpSections(AtunkoBindings.EXPAND, AtunkoBindings.SELECT_ALL, AtunkoBindings.OPEN_RUN);

        assertThat(sections)
                .extracting(HelpOverlay.Section::title)
                .containsExactly("Navigation", "Selection", "Actions");
    }

    @Test
    void unknownActionsAreSkippedRatherThanThrowing() {
        assertThat(AtunkoBindings.hintsFor("ATUNKO_DOES_NOT_EXIST")).isEmpty();
        assertThat(AtunkoBindings.helpSections("ATUNKO_DOES_NOT_EXIST")).isEmpty();
    }

    private static KeyEvent keyEvent(char c) {
        return KeyEvent.ofChar(c);
    }
}

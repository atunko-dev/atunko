package io.github.atunkodev.tui.shell;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards the documentation against the drift that motivated this change: {@code README.md} documented {@code c} for
 * collapse when nothing bound it, and described {@code a} as a three-way cycle when the code has {@code a} and
 * {@code A}.
 *
 * <p>No TamboUI consumer generates docs from bindings, and the framework has no facility for it (tamboui#168), so
 * this asserts agreement rather than generating the tables — the fallback the design doc names.
 *
 * <p>Only single-character keys are checked. Multi-key cells like {@code Ctrl+↑} and combinations documented as
 * {@code Space/Enter} describe framework bindings atunko does not own.
 */
class DocumentedKeysMatchRegistryTest {

    /** Repo root, found by walking up from the module — tests run with the module as working directory. */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        return dir;
    }

    static Stream<Path> documentationFiles() {
        Path root = repoRoot();
        return root == null
                ? Stream.of()
                : Stream.of(root.resolve("README.md"), root.resolve("docs/antora/modules/ROOT/pages/cli.adoc"));
    }

    /** Keys the framework's standard binding set owns; atunko's registry does not redeclare them. */
    private static final Set<String> FRAMEWORK_KEYS =
            Set.of("Enter", "Space", "Esc", "Tab", "Shift+Tab", "↑", "↓", "→", "←", "+", "-");

    @ParameterizedTest
    @MethodSource("documentationFiles")
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void everyDocumentedLetterKeyIsInTheRegistry(Path file) throws IOException {
        Assumptions.assumeTrue(Files.exists(file), "documentation file not present: " + file);

        Set<String> registryKeys = new LinkedHashSet<>();
        for (AtunkoBindings.Binding binding : AtunkoBindings.all()) {
            for (String part : binding.hintKey().split("/")) {
                registryKeys.add(part.strip());
            }
        }

        Set<String> documented = documentedSingleCharKeys(file);
        assertThat(documented)
                .as("no key cells found — the parser has drifted from the doc format")
                .isNotEmpty();

        assertThat(documented)
                .as("%s documents keys the binding registry does not declare", file.getFileName())
                .allSatisfy(key -> assertThat(registryKeys.contains(key) || FRAMEWORK_KEYS.contains(key))
                        .as("key `%s` is documented but not bound", key)
                        .isTrue());
    }

    /**
     * Pulls `x` cells out of the Markdown and AsciiDoc key tables.
     *
     * <p>Table rows only. Prose mentions keys too — this file's own "`j` / `k` are not bound" note being the
     * obvious example — and a note explaining that a key does nothing must not be read as a claim that it does.
     */
    private static Set<String> documentedSingleCharKeys(Path file) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.strip();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            Matcher matcher = Pattern.compile("`([^`]{1,2})`").matcher(trimmed);
            while (matcher.find()) {
                String candidate = matcher.group(1).strip();
                // Letters only: short backticked fragments in table prose are not all keys.
                if (candidate.length() == 1 && Character.isLetter(candidate.charAt(0))) {
                    keys.add(candidate);
                }
            }
        }
        return keys;
    }

    /**
     * Sanity check that the registry itself is non-trivial, so an empty registry cannot make the above vacuous.
     *
     * <p>Plain {@code @Test}: reqstool cannot recover a method name from a parameterised result, so an SVC carried
     * only by the parameterised case above would read as "automated test missing".
     */
    @Test
    @SVCs({"atunko:SVC_TUI_0009.6"})
    void theRegistryDeclaresKeys() {
        assertThat(AtunkoBindings.all()).hasSizeGreaterThan(15);
    }
}

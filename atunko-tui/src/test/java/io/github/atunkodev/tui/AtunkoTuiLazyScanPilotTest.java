package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.event.KeyCode;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ParsedSourcesCache;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectScanner;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;

/**
 * End-to-end verification that the TUI launches on a session whose project has not been scanned, runs the scan on the
 * first execution, and reports a failing scan inside the interface instead of dying at startup.
 */
@SVCs({"atunko:SVC_TUI_0005"})
class AtunkoTuiLazyScanPilotTest {

    private static final RecipeInfo RECIPE = new RecipeInfo(
            "org.openrewrite.java.RemoveUnusedImports",
            "Remove unused imports",
            "Removes imports that are not referenced",
            java.util.Set.of("java"));

    /** Stands in for a real build-system scan: counts its calls and can be told to fail. */
    private static final class RecordingScanner implements ProjectScanner {
        private final AtomicInteger calls = new AtomicInteger();
        private final boolean fail;

        RecordingScanner(boolean fail) {
            this.fail = fail;
        }

        @Override
        public ProjectInfo scan(Path projectDir) {
            calls.incrementAndGet();
            if (fail) {
                throw new IllegalStateException("Could not connect to the Gradle daemon");
            }
            return new ProjectInfo(List.of(), List.of(projectDir));
        }
    }

    @AfterEach
    void resetSession() {
        SessionHolder.init(Path.of("."), null);
    }

    /** Keeps the test about the deferred scan rather than about what a real recipe does to the fixture. */
    private static final class NoopEngine extends RecipeExecutionEngine {
        @Override
        public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
            return new ExecutionResult(List.of());
        }
    }

    /** A run parses the project on the event thread, so give it a moment to land before asserting. */
    private static void awaitScreen(PilotTestSupport tui, Screen expected) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            if (tui.controller().currentScreen() == expected) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Screen never became " + expected + ", still "
                + tui.controller().currentScreen());
    }

    private static TuiController controllerFor(Path projectDir) {
        return new TuiController(
                List.of(RECIPE),
                new RunConfigService(),
                new NoopEngine(),
                new ParsedSourcesCache(new ProjectSourceParser()),
                null,
                projectDir);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0005"})
    void tuiLaunchesAndBrowsesRecipesBeforeTheProjectIsScanned(@TempDir Path projectDir) throws Exception {
        RecordingScanner scanner = new RecordingScanner(false);
        SessionHolder.initLazy(projectDir, scanner);

        try (PilotTestSupport tui = PilotTestSupport.launch(controllerFor(projectDir))) {
            assertThat(tui.screen()).contains("Remove unused imports");
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.BROWSER);
            assertThat(scanner.calls).hasValue(0);
            assertThat(SessionHolder.getProjectInfo()).isNull();
            assertThat(SessionHolder.getProjectDir()).isEqualTo(projectDir);
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0005"})
    void firstRunTriggersTheScanAndExecutes(@TempDir Path projectDir) throws Exception {
        Files.writeString(projectDir.resolve("Sample.java"), "class Sample {}\n");
        RecordingScanner scanner = new RecordingScanner(false);
        SessionHolder.initLazy(projectDir, scanner);

        try (PilotTestSupport tui = PilotTestSupport.launch(controllerFor(projectDir))) {
            tui.pilot().press(' ');
            tui.pilot().press('r');
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.CONFIRM_RUN);

            tui.pilot().press('d');
            awaitScreen(tui, Screen.EXECUTION_RESULTS);

            assertThat(scanner.calls).hasValue(1);
            assertThat(SessionHolder.getProjectInfo()).isNotNull();
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.EXECUTION_RESULTS);
            assertThat(tui.controller().executionError()).isEmpty();
            assertThat(tui.controller().executionResult()).isPresent();
            tui.pilot().press(KeyCode.DOWN); // force a re-render through the live pipeline
            assertThat(tui.screen()).contains("Dry-Run Preview");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0005.1"})
    void aFailingScanIsShownAsAnExecutionErrorAndTheTuiSurvives(@TempDir Path projectDir) throws Exception {
        RecordingScanner scanner = new RecordingScanner(true);
        SessionHolder.initLazy(projectDir, scanner);

        try (PilotTestSupport tui = PilotTestSupport.launch(controllerFor(projectDir))) {
            tui.pilot().press(' ');
            tui.pilot().press('r');
            tui.pilot().press('d');
            awaitScreen(tui, Screen.EXECUTION_RESULTS);

            assertThat(tui.controller().executionError())
                    .hasValueSatisfying(msg -> assertThat(msg).contains("Project scan failed"));
            assertThat(tui.screen()).contains("Execution Failed");
            assertThat(tui.screen()).contains("Could not connect to the Gradle daemon");

            // the session survives — the browser is still reachable and the scan is retried on the next run
            tui.pilot().press('q');
            awaitScreen(tui, Screen.BROWSER);
            tui.pilot().press('r');
            tui.pilot().press('d');
            awaitScreen(tui, Screen.EXECUTION_RESULTS);
            assertThat(scanner.calls).hasValue(2);
        }
    }
}

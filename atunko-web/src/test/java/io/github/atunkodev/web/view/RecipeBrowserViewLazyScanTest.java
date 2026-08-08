package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.clearNotifications;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.button.Button;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectScanner;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

/**
 * Verifies that the Web UI works on a session whose project has not been scanned yet, runs the deferred scan on the
 * execution path, and reports a failing scan through the existing error notification without breaking the view.
 */
@SVCs({"atunko:SVC_WEB_0004"})
class RecipeBrowserViewLazyScanTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java"));

    private static final Routes ROUTES = new Routes().autoDiscoverViews("io.github.atunkodev.web");

    /** Stands in for a real build-system scan: counts its calls and can be told to fail. */
    private static final class RecordingScanner implements ProjectScanner {
        private final AtomicInteger calls = new AtomicInteger();
        private final boolean fail;
        private final CountDownLatch entered = new CountDownLatch(1);

        RecordingScanner(boolean fail) {
            this.fail = fail;
        }

        @Override
        public ProjectInfo scan(Path projectDir) {
            calls.incrementAndGet();
            entered.countDown();
            if (fail) {
                throw new IllegalStateException("Could not connect to the Gradle daemon");
            }
            return new ProjectInfo(List.of(), List.of(projectDir));
        }
    }

    @BeforeEach
    void resetServices() {
        AppServices.init(
                new RecipeExecutionEngine() {
                    @Override
                    public ExecutionResult execute(String recipeName, List<SourceFile> sources) {
                        return new ExecutionResult(List.of());
                    }
                },
                new ProjectSourceParser() {
                    @Override
                    public List<SourceFile> parse(ProjectInfo info) {
                        return List.of();
                    }
                },
                null);
        AppServices.setSourceCapabilities(Set.of());
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
        SessionHolder.init(Path.of("."), null);
    }

    private RecipeBrowserView setupView() {
        RecipeHolder.init(List.of(ALPHA));
        MockVaadin.setup(ROUTES);
        return _get(RecipeBrowserView.class);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0004"})
    void viewOpensOnAnUnscannedSessionWithoutTriggeringTheScan() {
        RecordingScanner scanner = new RecordingScanner(false);
        Path projectDir = Path.of("/lazy/project");
        SessionHolder.initLazy(projectDir, scanner);

        RecipeBrowserView view = setupView();

        assertThat(view.getDryRunButton().isEnabled()).isTrue();
        assertThat(scanner.calls).hasValue(0);
        assertThat(SessionHolder.getProjectInfo()).isNull();
        assertThat(SessionHolder.getProjectDir()).isEqualTo(projectDir);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0004"})
    void firstRunTriggersTheDeferredScan() throws Exception {
        RecordingScanner scanner = new RecordingScanner(false);
        SessionHolder.initLazy(Path.of("/lazy/project"), scanner);

        RecipeBrowserView view = setupView();
        view.selectAllVisible();

        _click(_get(Button.class, spec -> spec.withText("Dry Run")));
        _click(_get(Button.class, spec -> spec.withText("Confirm")));

        assertThat(scanner.entered.await(5, TimeUnit.SECONDS)).isTrue();
        awaitButtonsReEnabled(view);

        assertThat(scanner.calls).hasValue(1);
        assertThat(SessionHolder.getProjectInfo()).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0004"})
    void aFailingScanShowsAnErrorNotificationAndLeavesTheViewUsable() throws Exception {
        RecordingScanner scanner = new RecordingScanner(true);
        SessionHolder.initLazy(Path.of("/lazy/project"), scanner);

        RecipeBrowserView view = setupView();
        view.selectAllVisible();
        clearNotifications();

        _click(_get(Button.class, spec -> spec.withText("Dry Run")));
        _click(_get(Button.class, spec -> spec.withText("Confirm")));

        assertThat(scanner.entered.await(5, TimeUnit.SECONDS)).isTrue();
        awaitButtonsReEnabled(view);

        expectNotifications("Error: Project scan failed: Could not connect to the Gradle daemon");
        // the view survives — the scan was not memoized, so the next run tries again
        assertThat(view.getExecuteButton().isEnabled()).isTrue();
        assertThat(SessionHolder.getProjectInfo()).isNull();
    }

    /** Execution runs on a background thread and reports back through {@code UI.access}. */
    private static void awaitButtonsReEnabled(RecipeBrowserView view) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            MockVaadin.runUIQueue();
            if (view.getDryRunButton().isEnabled()) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Execution never finished — buttons stayed disabled");
    }
}

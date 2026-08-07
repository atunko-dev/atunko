package io.github.atunkodev.tui;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.toolkit.app.ToolkitPilot;
import dev.tamboui.toolkit.app.ToolkitPostRenderProcessor;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.pilot.Pilot;
import io.github.atunkodev.core.recipe.RecipeInfo;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Launches the real {@link AtunkoTui} headlessly and exposes the {@link Pilot} driving it plus the text of the last
 * rendered frame. Lives in this package to reach the protected {@link AtunkoTui#render()}.
 *
 * <p>Builds its own {@link ToolkitRunner} instead of using TamboUI's {@code ToolkitTestRunner}: that runner offers no
 * way to observe frame content ({@code TestBackend.draw()} is a no-op, so {@code rawOutput()} stays empty) and cannot
 * carry the TCSS {@link StyleEngine}. Rendering here uses the production dark theme, and a post-render processor
 * snapshots each frame's buffer for {@link #screen()} assertions.
 */
final class PilotTestSupport implements AutoCloseable {

    static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java", "testing"));
    static final RecipeInfo BETA = new RecipeInfo("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));
    static final RecipeInfo GAMMA = new RecipeInfo("org.test.Gamma", "Gamma Recipe", "Third recipe", Set.of("java"));
    static final List<RecipeInfo> RECIPES = List.of(ALPHA, BETA, GAMMA);

    // 80x24 truncates recipe names in the browser table; 120x40 keeps screen() assertions reliable
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final TuiController controller;
    private final ToolkitRunner runner;
    private final ToolkitPilot pilot;
    private final FrameCapture capture;
    private final Thread runnerThread;

    private PilotTestSupport(
            TuiController controller,
            ToolkitRunner runner,
            ToolkitPilot pilot,
            FrameCapture capture,
            Thread runnerThread) {
        this.controller = controller;
        this.runner = runner;
        this.pilot = pilot;
        this.capture = capture;
        this.runnerThread = runnerThread;
    }

    static PilotTestSupport launch() throws Exception {
        return launch(RECIPES);
    }

    static PilotTestSupport launch(List<RecipeInfo> recipes) throws Exception {
        TuiController controller = new TuiController(recipes);
        AtunkoTui app = new AtunkoTui(controller);

        TestBackend backend = new TestBackend(WIDTH, HEIGHT);
        // Same test tuning as TamboUI's own ToolkitTestRunner: headless backend, no raw mode, event-driven rendering
        TuiConfig config = TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .mouseCapture(true)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .noTick()
                .backend(backend)
                .build();

        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("dark", "/themes/dark.tcss");
        styleEngine.loadStylesheet("light", "/themes/light.tcss");
        styleEngine.setActiveStylesheet("dark");

        FrameCapture capture = new FrameCapture();
        ToolkitRunner runner = ToolkitRunner.builder()
                .config(config)
                .styleEngine(styleEngine)
                .postRenderProcessor(capture)
                .build();
        ToolkitPilot pilot = new ToolkitPilot(runner, backend);

        Thread runnerThread = new Thread(
                () -> {
                    try {
                        runner.run(app::render);
                    } catch (Exception e) {
                        throw new IllegalStateException("Headless TUI run failed", e);
                    }
                },
                "atunko-pilot-tui");
        runnerThread.setDaemon(true);
        runnerThread.start();

        PilotTestSupport support = new PilotTestSupport(controller, runner, pilot, capture, runnerThread);
        support.awaitFirstFrame();
        return support;
    }

    TuiController controller() {
        return controller;
    }

    Pilot pilot() {
        return pilot;
    }

    /** Returns the text content of the most recently rendered frame. */
    String screen() {
        return capture.text;
    }

    /** Dispatches a raw event into the running TUI — e.g. mouse scroll, which {@link Pilot} has no method for. */
    void dispatch(Event event) {
        runner.tuiRunner().dispatch(event);
        pilot.pause();
    }

    private void awaitFirstFrame() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (!capture.text.isEmpty() && pilot.hasElement("browser")) {
                return;
            }
            sleep(10);
        }
        throw new IllegalStateException("Headless TUI did not render a first frame within 5s");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        pilot.quit();
        try {
            runnerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        runner.close();
    }

    private static final class FrameCapture implements ToolkitPostRenderProcessor {

        private volatile String text = "";

        @Override
        public void process(
                Frame frame,
                ElementRegistry elementRegistry,
                StyledAreaRegistry styledAreaRegistry,
                FocusManager focusManager,
                Duration elapsed) {
            Buffer buffer = frame.buffer();
            StringBuilder sb = new StringBuilder(buffer.height() * (buffer.width() + 1));
            for (int y = 0; y < buffer.height(); y++) {
                for (int x = 0; x < buffer.width(); x++) {
                    sb.append(buffer.get(x, y).symbol());
                }
                sb.append('\n');
            }
            text = sb.toString();
        }
    }
}

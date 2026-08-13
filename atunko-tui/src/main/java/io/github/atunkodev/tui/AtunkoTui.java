package io.github.atunkodev.tui;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.error.ErrorAction;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiShell;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.atunkodev.tui.view.BrowserView;
import io.github.atunkodev.tui.view.ConfirmRunView;
import io.github.atunkodev.tui.view.DetailView;
import io.github.atunkodev.tui.view.ExecutionResultsView;
import io.github.atunkodev.tui.view.FileDiffView;
import io.github.atunkodev.tui.view.HelpOverlay;
import io.github.atunkodev.tui.view.LoadConfigView;
import io.github.atunkodev.tui.view.TagBrowserView;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Requirements({"atunko:TUI_0001", "atunko:TUI_0001.15"})
public class AtunkoTui extends ToolkitApp {

    private final TuiController controller;
    private final Path logFile;
    private final ThemeConfig themeConfig;

    /**
     * The runner driving this app. {@link ToolkitApp} keeps its own, but only its {@code run()} assigns it, and this
     * class overrides {@code run()} to pass the TCSS style engine — so the inherited {@code quit()} is a no-op.
     */
    private ToolkitRunner activeRunner;

    public AtunkoTui(TuiController controller) {
        this(controller, null, ThemeConfig.DEFAULT);
    }

    public AtunkoTui(TuiController controller, Path logFile, ThemeConfig themeConfig) {
        this.controller = controller;
        this.logFile = logFile;
        this.themeConfig = themeConfig;
        if (logFile != null) {
            configureLogging(logFile);
        }
    }

    @Override
    protected Element render() {
        return renderThroughShell(viewFor());
    }

    /**
     * The screen currently selected, as a {@link TuiView}. Exhaustive over {@link Screen} by construction — a new
     * screen will not compile until it has a view, which is what keeps every screen inside the shared frame.
     */
    public TuiView viewFor() {
        return switch (controller.currentScreen()) {
            case BROWSER -> new BrowserView(this);
            case DETAIL -> new DetailView();
            case TAG_BROWSER -> new TagBrowserView();
            case EXECUTION_RESULTS, WORKSPACE_RESULTS -> new ExecutionResultsView();
            case FILE_DIFF -> new FileDiffView();
            case CONFIRM_RUN -> new ConfirmRunView();
            case LOAD_CONFIG -> new LoadConfigView();
        };
    }

    /**
     * Renders a migrated screen inside the shared frame, opening help as a true overlay over its content rather
     * than replacing it.
     */
    @Requirements({"atunko:TUI_0009", "atunko:TUI_0009.4"})
    private Element renderThroughShell(TuiView view) {
        if (controller.isShowHelp()) {
            return TuiShell.render(
                    view,
                    controller,
                    HelpOverlay.render(view.helpSections()),
                    java.util.List.of(KeyHint.of("any key", "close help")));
        }
        return TuiShell.render(view, controller);
    }

    @Override
    @Requirements({"atunko:TUI_0001.27"})
    protected TuiConfig configure() {
        if (logFile != null) {
            return TuiConfig.builder()
                    .mouseCapture(true)
                    .errorHandler((error, context) -> {
                        Logger.getLogger("io.github.atunkodev").log(Level.SEVERE, "Render error", error.cause());
                        return ErrorAction.QUIT_IMMEDIATELY;
                    })
                    .build();
        }
        return TuiConfig.builder().mouseCapture(true).build();
    }

    @Override
    public void run() throws Exception {
        StyleEngine styleEngine = createStyleEngine();
        try (ToolkitRunner r = ToolkitRunner.builder()
                .config(configure())
                .styleEngine(styleEngine)
                .build()) {
            bindRunner(r);
            onStart();
            r.run(this::render);
        } finally {
            bindRunner(null);
            onStop();
        }
    }

    /**
     * Binds the runner {@link #requestQuit()} quits. Called by {@link #run()}; a harness that builds its own runner
     * must call it too, otherwise it tests a quit path the app does not use.
     */
    public void bindRunner(ToolkitRunner runner) {
        this.activeRunner = runner;
    }

    @Requirements({"atunko:TUI_0001.28"})
    public void requestQuit() {
        if (activeRunner != null) {
            activeRunner.quit();
        }
    }

    @Requirements({"atunko:TUI_0001.18"})
    private StyleEngine createStyleEngine() throws IOException {
        StyleEngine engine = StyleEngine.create();
        if (themeConfig.isUserCss()) {
            engine.loadStylesheet(themeConfig.cssFile());
        } else {
            engine.loadStylesheet("dark", "/themes/dark.tcss");
            engine.loadStylesheet("light", "/themes/light.tcss");
            engine.setActiveStylesheet(themeConfig.themeName());
        }
        return engine;
    }

    private static void configureLogging(Path logFile) {
        try {
            Logger logger = Logger.getLogger("io.github.atunkodev");
            FileHandler fh = new FileHandler(logFile.toString());
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.FINE);
        } catch (IOException e) {
            System.err.println("Warning: could not open log file " + logFile + ": " + e.getMessage());
        }
    }
}

package io.github.atunkodev.tui;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.error.ErrorAction;
import io.github.atunkodev.tui.view.BrowserView;
import io.github.atunkodev.tui.view.ConfirmRunView;
import io.github.atunkodev.tui.view.DetailView;
import io.github.atunkodev.tui.view.ExecutionResultsView;
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
        return switch (controller.currentScreen()) {
            case BROWSER -> BrowserView.render(controller, this);
            case DETAIL -> DetailView.render(controller);
            case TAG_BROWSER -> TagBrowserView.render(controller);
            case EXECUTION_RESULTS -> ExecutionResultsView.render(controller);
            case CONFIRM_RUN -> ConfirmRunView.render(controller);
        };
    }

    @Override
    protected TuiConfig configure() {
        if (logFile != null) {
            return TuiConfig.builder()
                    .errorHandler((error, context) -> {
                        Logger.getLogger("io.github.atunkodev").log(Level.SEVERE, "Render error", error.cause());
                        return ErrorAction.QUIT_IMMEDIATELY;
                    })
                    .build();
        }
        return TuiConfig.defaults();
    }

    @Override
    public void run() throws Exception {
        StyleEngine styleEngine = createStyleEngine();
        try (ToolkitRunner r = ToolkitRunner.builder()
                .config(configure())
                .styleEngine(styleEngine)
                .build()) {
            onStart();
            r.run(this::render);
        } finally {
            onStop();
        }
    }

    public void requestQuit() {
        quit();
    }

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

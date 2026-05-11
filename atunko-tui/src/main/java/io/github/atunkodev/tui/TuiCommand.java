package io.github.atunkodev.tui;

import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectScannerFactory;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "tui", description = "Launch interactive TUI for recipe browsing and execution")
public class TuiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;
    private final RunConfigService runConfigService;
    private final RecipeExecutionEngine engine;
    private final ProjectSourceParser sourceParser;
    private final ChangeApplier changeApplier;

    @Option(names = "--project-dir", description = "Project directory", defaultValue = ".")
    private Path projectDir;

    @Option(names = "--log-file", description = "Log file for TUI debug output")
    private Path logFile;

    @Option(names = "--theme", description = "Theme name: dark (default), light")
    private String theme;

    @Option(names = "--css-file", description = "Path to a custom CSS theme file (replaces bundled theme)")
    private Path cssFile;

    public TuiCommand(
            RecipeDiscoveryService discoveryService,
            RunConfigService runConfigService,
            RecipeExecutionEngine engine,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier) {
        this.discoveryService = discoveryService;
        this.runConfigService = runConfigService;
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
    }

    @Override
    @Requirements({"atunko:TUI_0001"})
    public void run() {
        SessionHolder.init(projectDir, ProjectScannerFactory.detect(projectDir).scan(projectDir));
        List<RecipeInfo> recipes = discoveryService.discoverAll();
        TuiController controller =
                new TuiController(recipes, runConfigService, engine, sourceParser, changeApplier, projectDir);
        ThemeConfig themeConfig = ThemeConfig.resolve(theme, cssFile);
        AtunkoTui tui = new AtunkoTui(controller, logFile, themeConfig);
        try {
            tui.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start TUI", e);
        }
    }
}

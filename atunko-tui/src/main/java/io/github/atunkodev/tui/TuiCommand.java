package io.github.atunkodev.tui;

import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.engine.WorkspaceExecutionEngine;
import io.github.atunkodev.core.project.ParsedSourcesCache;
import io.github.atunkodev.core.project.ProjectScannerFactory;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.project.WorkspaceScanner;
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

    @Option(names = "--project-dir", description = "Project directory (mutually exclusive with --workspace)")
    private Path projectDir;

    @Option(
            names = "--workspace",
            description = "Workspace root — scans for all projects underneath (mutually exclusive with --project-dir)")
    private Path workspaceDir;

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
    @Requirements({"atunko:TUI_0001", "atunko:TUI_0002", "atunko:TUI_0002.1", "atunko:TUI_0005"})
    public void run() {
        List<RecipeInfo> recipes = discoveryService.discoverAll();
        // The one LST cache of this TUI session — shared by the controller and the workspace engine so a
        // project parsed by either is a cache hit for the other.
        ParsedSourcesCache sourceCache = sourceParser != null ? new ParsedSourcesCache(sourceParser) : null;
        TuiController controller;
        if (workspaceDir != null) {
            Workspace workspace = WorkspaceScanner.scan(workspaceDir);
            SessionHolder.initWorkspace(workspace.root(), workspace.projects());
            WorkspaceExecutionEngine workspaceEngine = new WorkspaceExecutionEngine(engine, sourceCache);
            controller = new TuiController(
                    recipes, runConfigService, engine, sourceCache, changeApplier, workspaceEngine, workspaceDir);
        } else {
            Path dir = projectDir != null ? projectDir : Path.of(".");
            // Detect fails fast on a directory without build files, but the scan itself is the
            // expensive part and is deferred to the first recipe execution.
            ProjectScannerFactory.detect(dir);
            SessionHolder.initLazy(dir);
            controller = new TuiController(recipes, runConfigService, engine, sourceCache, changeApplier, dir);
        }
        ThemeConfig themeConfig = ThemeConfig.resolve(theme, cssFile);
        AtunkoTui tui = new AtunkoTui(controller, logFile, themeConfig);
        try {
            tui.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start TUI", e);
        }
    }
}

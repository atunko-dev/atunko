package io.github.atunkodev.web;

import com.github.mvysny.vaadinboot.VaadinBoot;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectScannerFactory;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.project.SourceCapabilityHints;
import io.github.atunkodev.core.project.Workspace;
import io.github.atunkodev.core.project.WorkspaceScanner;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "webui", description = "Launch browser-based recipe browser")
public class WebUiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;
    private final RecipeExecutionEngine engine;
    private final ProjectSourceParser sourceParser;
    private final ChangeApplier changeApplier;

    @Option(names = "--port", description = "Port to listen on (default: 8080)", defaultValue = "8080")
    private int port = 8080;

    @Option(names = "--project-dir", description = "Project directory (default: current directory)", defaultValue = ".")
    private Path projectDir = Path.of(".");

    @Option(names = "--workspace", description = "Workspace root directory — scans for all projects underneath")
    private Path workspaceDir;

    public WebUiCommand(
            RecipeDiscoveryService discoveryService,
            RecipeExecutionEngine engine,
            ProjectSourceParser sourceParser,
            ChangeApplier changeApplier) {
        this.discoveryService = discoveryService;
        this.engine = engine;
        this.sourceParser = sourceParser;
        this.changeApplier = changeApplier;
    }

    public int getPort() {
        return port;
    }

    public Path getProjectDir() {
        return projectDir;
    }

    @Override
    @Requirements({
        "atunko:WEB_0001",
        "atunko:WEB_0001.3",
        "atunko:WEB_0001.7",
        "atunko:WEB_0002",
        "atunko:WEB_0002.4",
        "atunko:WEB_0004"
    })
    public void run() {
        RecipeHolder.init(discoveryService.discoverAll());
        if (workspaceDir != null) {
            Workspace workspace =
                    WorkspaceScanner.scan(workspaceDir.toAbsolutePath().normalize());
            SessionHolder.initWorkspace(workspace.root(), workspace.projects());
        } else {
            // Detect fails fast on a directory without build files, but the scan itself is the
            // expensive part and is deferred to the first recipe execution.
            ProjectScannerFactory.detect(projectDir);
            SessionHolder.initLazy(projectDir);
        }
        AppServices.init(engine, sourceParser, changeApplier);
        // Badges are shown before anything is parsed, so start from the project-type hint; the first run replaces
        // this with the capabilities the parse actually produced.
        AppServices.setSourceCapabilities(
                SourceCapabilityHints.forProjectDir(workspaceDir != null ? workspaceDir : projectDir));
        try {
            new VaadinBoot().withPort(port).run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Web UI", e);
        }
    }
}

package io.github.atunkodev.tui;

import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import picocli.CommandLine.Command;

@Command(name = "tui", description = "Launch interactive TUI for recipe browsing and execution")
public class TuiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;
    private final RunConfigService runConfigService;

    public TuiCommand(RecipeDiscoveryService discoveryService, RunConfigService runConfigService) {
        this.discoveryService = discoveryService;
        this.runConfigService = runConfigService;
    }

    @Override
    @Requirements({"CLI_0001"})
    public void run() {
        List<RecipeInfo> recipes = discoveryService.discoverAll();
        TuiController controller = new TuiController(recipes, runConfigService);
        AtunkoTui tui = new AtunkoTui(controller);
        try {
            tui.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start TUI", e);
        }
    }
}

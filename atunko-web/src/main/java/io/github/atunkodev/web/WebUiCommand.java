package io.github.atunkodev.web;

import com.github.mvysny.vaadinboot.VaadinBoot;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "webui", description = "Launch browser-based recipe browser")
public class WebUiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;

    @Option(names = "--port", description = "Port to listen on (default: 8080)", defaultValue = "8080")
    private int port = 8080;

    @Option(names = "--project-dir", description = "Project directory (default: current directory)", defaultValue = ".")
    private Path projectDir = Path.of(".");

    public WebUiCommand(RecipeDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public int getPort() {
        return port;
    }

    public Path getProjectDir() {
        return projectDir;
    }

    @Override
    @Requirements({"atunko:WEB_0001", "atunko:WEB_0001.3", "atunko:WEB_0001.7"})
    public void run() {
        RecipeHolder.init(discoveryService.discoverAll(), projectDir);
        try {
            new VaadinBoot().withPort(port).run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Web UI", e);
        }
    }
}

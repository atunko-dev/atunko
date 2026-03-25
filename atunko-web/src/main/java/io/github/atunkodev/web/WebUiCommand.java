package io.github.atunkodev.web;

import com.github.mvysny.vaadinboot.VaadinBoot;
import com.github.mvysny.vaadinboot.common.WebServer;
import com.vaadin.open.Open;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "webui", description = "Launch browser-based recipe browser")
public class WebUiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;

    @Option(names = "--port", description = "Port to listen on (default: 8080)", defaultValue = "8080")
    private int port = 8080;

    public WebUiCommand(RecipeDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public int getPort() {
        return port;
    }

    @Override
    @Requirements({"atunko:WEB_0001"})
    public void run() {
        RecipeHolder.init(discoveryService.discoverAll());
        try {
            new VaadinBoot() {
                @Override
                protected void onStarted(WebServer server) throws IOException {
                    super.onStarted(server);
                    Open.open("http://localhost:" + port);
                }
            }.withPort(port).run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Web UI", e);
        }
    }
}

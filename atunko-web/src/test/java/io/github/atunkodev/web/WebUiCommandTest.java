package io.github.atunkodev.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class WebUiCommandTest {

    private static RecipeDiscoveryService emptyDiscovery() {
        return new RecipeDiscoveryService() {
            @Override
            public java.util.List<io.github.atunkodev.core.recipe.RecipeInfo> discoverAll() {
                return List.of();
            }
        };
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001"})
    void command_canBeInstantiated() {
        WebUiCommand command = new WebUiCommand(emptyDiscovery());
        assertThat(command).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.3"})
    void command_defaultPortIs8080() {
        WebUiCommand command = new WebUiCommand(emptyDiscovery());
        assertThat(command.getPort()).isEqualTo(8080);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.3"})
    void command_customPortIsApplied() {
        WebUiCommand command = new WebUiCommand(emptyDiscovery());
        new CommandLine(command).parseArgs("--port", "9090");
        assertThat(command.getPort()).isEqualTo(9090);
    }
}

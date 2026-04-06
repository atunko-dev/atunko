package io.github.atunkodev.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
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

    private static WebUiCommand newCommand() {
        return new WebUiCommand(emptyDiscovery(), null, null, null);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001"})
    void commandCanBeInstantiated() {
        assertThat(newCommand()).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.3"})
    void commandDefaultPortIs8080() {
        assertThat(newCommand().getPort()).isEqualTo(8080);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.3"})
    void commandCustomPortIsApplied() {
        WebUiCommand command = newCommand();
        new CommandLine(command).parseArgs("--port", "9090");
        assertThat(command.getPort()).isEqualTo(9090);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.10"})
    void commandDefaultProjectDirIsCurrent() {
        assertThat(newCommand().getProjectDir()).isEqualTo(Path.of("."));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.11"})
    void commandCustomProjectDirIsApplied() {
        WebUiCommand command = newCommand();
        new CommandLine(command).parseArgs("--project-dir", "/some/project");
        assertThat(command.getProjectDir()).isEqualTo(Path.of("/some/project"));
    }
}

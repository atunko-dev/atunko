package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtunkoTuiTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java"));
    private static final List<RecipeInfo> RECIPES = List.of(ALPHA);

    @Test
    @SVCs({"atunko:SVC_TUI_0001.15"})
    void logFileConfiguresFileHandler(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("debug.log");
        TuiController controller = new TuiController(RECIPES);

        new AtunkoTui(controller, logFile);

        Logger logger = Logger.getLogger("io.github.atunkodev");
        assertThat(logger.getLevel()).isNotNull();
        assertThat(logger.getHandlers()).anyMatch(h -> h instanceof java.util.logging.FileHandler);
    }
}

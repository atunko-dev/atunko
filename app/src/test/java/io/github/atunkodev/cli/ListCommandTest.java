package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SVCs({"SVC_CLI_0002"})
class ListCommandTest {

    @Test
    @SVCs({"SVC_CLI_0002.1"})
    void list_displaysRecipesAsText() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe");
        assertThat(cli.stdout()).contains(" - ");
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"SVC_CLI_0002.2"})
    void list_displaysRecipesAsJson() throws Exception {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--format", "json");

        assertThat(exitCode).isZero();
        String output = cli.stdout().trim();
        assertThat(output).startsWith("[");
        assertThat(output).endsWith("]");

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> recipes = mapper.readValue(output, new TypeReference<>() {});
        assertThat(recipes).isNotEmpty();
        assertThat(recipes.get(0)).containsKeys("name", "displayName", "description", "tags");
    }

    @Test
    @SVCs({"SVC_CLI_0002.3"})
    void list_sortsByNameByDefault() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "name");

        assertThat(exitCode).isZero();
        String output = cli.stdout();
        String[] lines = output.split("\n");
        // Filter to recipe lines: start with a qualified name and contain " - "
        List<String> recipeLines = java.util.Arrays.stream(lines)
                .filter(l -> l.matches("[a-zA-Z]\\S* - .*"))
                .toList();
        assertThat(recipeLines).hasSizeGreaterThan(1);
        // Verify alphabetical order
        for (int i = 1; i < recipeLines.size(); i++) {
            assertThat(recipeLines.get(i).toLowerCase(Locale.ROOT))
                    .isGreaterThanOrEqualTo(recipeLines.get(i - 1).toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @SVCs({"SVC_CLI_0002.4"})
    void list_sortsByTags() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }
}

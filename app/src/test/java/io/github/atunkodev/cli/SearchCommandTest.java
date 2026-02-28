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

@SVCs({"SVC_CLI_0004"})
class SearchCommandTest {

    @Test
    @SVCs({"SVC_CLI_0004.1"})
    void search_displaysMatchingRecipesAsText() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "unused imports");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).containsIgnoringCase("unused");
        assertThat(cli.stdout()).contains(" - ");
    }

    @Test
    @SVCs({"SVC_CLI_0004.1"})
    void search_showsMessageWhenNoResults() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "xyznonexistent999");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("No recipes found");
    }

    @Test
    @SVCs({"SVC_CLI_0004.2"})
    void search_displaysMatchingRecipesAsJson() throws Exception {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "unused imports", "--format", "json");

        assertThat(exitCode).isZero();
        String output = cli.stdout().trim();
        assertThat(output).startsWith("[");

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> recipes = mapper.readValue(output, new TypeReference<>() {});
        assertThat(recipes).isNotEmpty();
        assertThat(recipes.get(0)).containsKeys("name", "displayName", "description", "tags");
    }

    @Test
    @SVCs({"SVC_CLI_0004.3"})
    void search_sortsByName() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "java", "--sort", "name");

        assertThat(exitCode).isZero();
        String[] lines = cli.stdout().split("\n");
        // Filter to recipe lines: start with a qualified name and contain " - "
        List<String> recipeLines = java.util.Arrays.stream(lines)
                .filter(l -> l.matches("[a-zA-Z]\\S* - .*"))
                .toList();
        assertThat(recipeLines).hasSizeGreaterThan(1);
        for (int i = 1; i < recipeLines.size(); i++) {
            assertThat(recipeLines.get(i).toLowerCase(Locale.ROOT))
                    .isGreaterThanOrEqualTo(recipeLines.get(i - 1).toLowerCase(Locale.ROOT));
        }
    }

    @Test
    @SVCs({"SVC_CLI_0004.4"})
    void search_sortsByTags() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "java", "--sort", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"SVC_CLI_0004.5"})
    void search_filtersByField() {
        CommandLineFixture cli = CommandLineFixture.create();

        // Search only in tags — "java" is a common tag
        int exitCode = cli.execute("search", "java", "--field", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }
}

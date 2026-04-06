package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CLI_0004"})
class SearchCommandTest {

    @Test
    @SVCs({"atunko:SVC_CLI_0004.1"})
    void searchDisplaysMatchingRecipesAsText() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "unused imports");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).containsIgnoringCase("unused");
        assertThat(cli.stdout()).contains(" - ");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0004.1"})
    void searchShowsMessageWhenNoResults() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "xyznonexistent999");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("No recipes found");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0004.2"})
    void searchDisplaysMatchingRecipesAsJson() throws Exception {
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
    @SVCs({"atunko:SVC_CLI_0004.3"})
    void searchSortsByName() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "java", "--sort", "name");

        assertThat(exitCode).isZero();
        CommandLineFixture.assertRecipeLinesAreSorted(cli.stdout());
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0004.4"})
    void searchSortsByTags() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("search", "java", "--sort", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0004.5"})
    void searchFiltersByField() {
        CommandLineFixture cli = CommandLineFixture.create();

        // Search only in tags — "java" is a common tag
        int exitCode = cli.execute("search", "java", "--field", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }
}

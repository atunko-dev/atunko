package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CLI_0002"})
class ListCommandTest {

    @Test
    @SVCs({"atunko:SVC_CLI_0002.1"})
    void listDisplaysRecipesAsText() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe");
        assertThat(cli.stdout()).contains(" - ");
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0002.2"})
    void listDisplaysRecipesAsJson() throws Exception {
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
    @SVCs({"atunko:SVC_CLI_0002.3"})
    void listSortsByNameByDefault() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "name");

        assertThat(exitCode).isZero();
        CommandLineFixture.assertRecipeLinesAreSorted(cli.stdout());
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0002.4"})
    void listSortsByTags() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--sort", "tags");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("recipe(s) found.");
    }
}

package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Verifies the JBang catalog at the repository root is a usable {@code atunko} alias definition. */
@SVCs({"atunko:SVC_CLI_0006"})
class JbangCatalogTest {

    private static final String EXPECTED_SCRIPT_REF =
            "https://github.com/atunko-dev/atunko/releases/download/snapshot/atunko.jar";

    private static JsonNode catalog;

    @BeforeAll
    static void readCatalog() throws IOException {
        Path catalogFile = repositoryRoot().resolve("jbang-catalog.json");
        assertThat(catalogFile).exists();
        catalog = new ObjectMapper().readTree(Files.readString(catalogFile));
    }

    /** Walks up from the working directory (the module dir under Gradle) to the repository root. */
    private static Path repositoryRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("settings.gradle"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Repository root (settings.gradle) not found above "
                + Paths.get("").toAbsolutePath());
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0006"})
    void catalogDefinesAtunkoAlias() {
        assertThat(catalog.path("aliases").isObject()).isTrue();
        assertThat(catalog.path("aliases").has("atunko")).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0006"})
    void aliasPointsAtLatestReleaseJar() {
        JsonNode alias = catalog.path("aliases").path("atunko");

        String scriptRef = alias.path("script-ref").asText();

        assertThat(scriptRef).isEqualTo(EXPECTED_SCRIPT_REF);
        assertThatCode(() -> URI.create(scriptRef).toURL()).doesNotThrowAnyException();
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0006"})
    void aliasHasDescriptionAndJavaVersion() {
        JsonNode alias = catalog.path("aliases").path("atunko");

        assertThat(alias.path("description").asText()).isNotBlank();
        assertThat(alias.path("java").asText()).isEqualTo("25+");
    }
}

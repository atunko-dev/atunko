package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigExportCommandTest {

    @TempDir
    Path tempDir;

    private Path writeConfig(String content) throws IOException {
        Path file = tempDir.resolve("run.yaml");
        Files.writeString(file, content);
        return file;
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportGradleOutputsRewriteBlock() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--gradle", "--file", config.toString());

        assertThat(exitCode).as("stderr: %s", cli.stderr()).isZero();
        assertThat(cli.stdout()).contains("rewrite {");
        assertThat(cli.stdout()).contains("activeRecipe(");
        assertThat(cli.stdout()).contains("\"org.openrewrite.java.cleanup.RemoveUnusedImports\"");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportMavenOutputsPluginBlock() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--maven", "--file", config.toString());

        assertThat(exitCode).as("stderr: %s", cli.stderr()).isZero();
        assertThat(cli.stdout()).contains("<groupId>org.openrewrite.maven</groupId>");
        assertThat(cli.stdout()).contains("<activeRecipes>");
        assertThat(cli.stdout()).contains("<recipe>org.openrewrite.java.cleanup.RemoveUnusedImports</recipe>");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportWithoutFormatFlagFails() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--file", config.toString());

        assertThat(exitCode).isNotZero();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportWithBothFormatsFails() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--gradle", "--maven", "--file", config.toString());

        assertThat(exitCode).isNotZero();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.33"})
    void fullGradleOutputsStandaloneBuildFile() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--gradle", "--full", "--file", config.toString());

        assertThat(exitCode).as("stderr: %s", cli.stderr()).isZero();
        assertThat(cli.stdout()).contains("plugins {");
        assertThat(cli.stdout()).contains("repositories {");
        assertThat(cli.stdout()).contains("rewrite {");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.34"})
    void fullMavenOutputsFullPomXml() throws IOException {
        Path config = writeConfig("version: 1\nrecipes:\n  - name: org.openrewrite.java.cleanup.RemoveUnusedImports\n");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("config", "export", "--maven", "--full", "--file", config.toString());

        assertThat(exitCode).as("stderr: %s", cli.stderr()).isZero();
        assertThat(cli.stdout()).contains("<?xml");
        assertThat(cli.stdout()).contains("<groupId>io.github.atunkodev</groupId>");
    }
}

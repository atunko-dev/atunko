package io.github.atunko.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;

class RecipeExecutionEngineTest {

    private final RecipeExecutionEngine engine = new RecipeExecutionEngine();

    private List<SourceFile> parseFixture() {
        Path fixtureDir = Path.of("src/test/resources/fixtures/java-with-unused-imports");
        return JavaParser.fromJavaVersion()
                .build()
                .parse(List.of(fixtureDir.resolve("Example.java")), fixtureDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());
    }

    @Test
    @SVCs({"SVC_CORE_0003"})
    void execute_removesUnusedImports() {
        List<SourceFile> sources = parseFixture();

        ExecutionResult result = engine.execute("org.openrewrite.java.RemoveUnusedImports", sources);

        assertThat(result.changes()).isNotEmpty();
        FileChange change = result.changes().getFirst();
        assertThat(change.before()).contains("import java.util.Map;");
        assertThat(change.before()).contains("import java.util.Set;");
        assertThat(change.after()).doesNotContain("import java.util.Map;");
        assertThat(change.after()).doesNotContain("import java.util.Set;");
        assertThat(change.after()).contains("import java.util.List;");
    }

    @Test
    @SVCs({"SVC_CORE_0003"})
    void execute_withUnknownRecipe_throwsIllegalArgumentException() {
        List<SourceFile> sources = parseFixture();

        assertThatThrownBy(() -> engine.execute("org.openrewrite.NonExistentRecipe", sources))
                .isInstanceOf(org.openrewrite.RecipeException.class)
                .hasMessageContaining("NonExistentRecipe");
    }

    @Test
    @SVCs({"SVC_CORE_0003"})
    void execute_withNoMatchingChanges_returnsEmptyResult() {
        // Parse a file that has no unused imports
        List<SourceFile> sources = JavaParser.fromJavaVersion()
                .build()
                .parse(new InMemoryExecutionContext(), "package com.example;\n\npublic class Clean {\n}\n")
                .collect(Collectors.toList());

        ExecutionResult result = engine.execute("org.openrewrite.java.RemoveUnusedImports", sources);

        assertThat(result.changes()).isEmpty();
    }
}

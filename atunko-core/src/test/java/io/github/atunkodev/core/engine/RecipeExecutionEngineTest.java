package io.github.atunkodev.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
                .toList();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003"})
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
    @SVCs({"atunko:SVC_CORE_0003"})
    void execute_withUnknownRecipe_throwsException() {
        List<SourceFile> sources = parseFixture();

        assertThatThrownBy(() -> engine.execute("org.openrewrite.NonExistentRecipe", sources))
                .isInstanceOf(Exception.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003"})
    void execute_withNoMatchingChanges_returnsEmptyResult() {
        List<SourceFile> sources = JavaParser.fromJavaVersion()
                .build()
                .parse(new InMemoryExecutionContext(), "package com.example;\n\npublic class Clean {\n}\n")
                .toList();

        ExecutionResult result = engine.execute("org.openrewrite.java.RemoveUnusedImports", sources);

        assertThat(result.changes()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0003.2"})
    void execute_withOptions_appliesOptionValues() {
        List<SourceFile> sources = JavaParser.fromJavaVersion()
                .build()
                .parse(new InMemoryExecutionContext(), """
                    package com.example;

                    public class Foo {
                        public void oldName() {}
                    }
                    """)
                .toList();

        ExecutionResult result = engine.execute(
                "org.openrewrite.java.ChangeMethodName",
                Map.of(
                        "methodPattern", "com.example.Foo oldName()",
                        "newMethodName", "newName"),
                sources);

        assertThat(result.changes()).isNotEmpty();
        FileChange change = result.changes().getFirst();
        assertThat(change.after()).contains("newName");
        assertThat(change.after()).doesNotContain("oldName");
    }
}

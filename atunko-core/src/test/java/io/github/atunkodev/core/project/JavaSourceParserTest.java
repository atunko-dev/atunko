package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void parsesJavaFilesFromValidDirectory() {
        Path fixtureDir = Path.of("src/test/resources/fixtures/java-with-unused-imports");

        var sources = parser.parse(fixtureDir);

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(source -> assertThat(source).isInstanceOf(SourceFile.class));
    }

    @Test
    void throwsForNonExistentDirectory() {
        Path noSuchDir = Path.of("src/test/resources/fixtures/does-not-exist");

        assertThatThrownBy(() -> parser.parse(noSuchDir)).isInstanceOf(IllegalArgumentException.class);
    }
}

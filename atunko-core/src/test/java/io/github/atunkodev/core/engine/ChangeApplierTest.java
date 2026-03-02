package io.github.atunkodev.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChangeApplierTest {

    private final ChangeApplier applier = new ChangeApplier();

    @Test
    void writesFileWhenAfterIsPresent(@TempDir Path tempDir) throws IOException {
        Path relativePath = Path.of("src/Main.java");
        FileChange change = new FileChange(relativePath, null, "class Main {}");

        applier.apply(tempDir, List.of(change));

        Path written = tempDir.resolve(relativePath);
        assertThat(written).exists();
        assertThat(Files.readString(written)).isEqualTo("class Main {}");
    }

    @Test
    void deletesFileWhenAfterIsNull(@TempDir Path tempDir) throws IOException {
        Path relativePath = Path.of("Obsolete.java");
        Path target = tempDir.resolve(relativePath);
        Files.writeString(target, "old content");

        FileChange change = new FileChange(relativePath, "old content", null);

        applier.apply(tempDir, List.of(change));

        assertThat(target).doesNotExist();
    }
}

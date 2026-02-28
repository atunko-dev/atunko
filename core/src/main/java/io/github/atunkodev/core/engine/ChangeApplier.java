package io.github.atunkodev.core.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ChangeApplier {

    public void apply(Path projectDir, List<FileChange> changes) {
        Path absoluteDir = projectDir.toAbsolutePath().normalize();
        for (FileChange change : changes) {
            applyChange(absoluteDir, change);
        }
    }

    private void applyChange(Path absoluteDir, FileChange change) {
        try {
            Path target = absoluteDir.resolve(change.path());
            if (change.after() != null) {
                Files.createDirectories(target.getParent());
                Files.writeString(target, change.after());
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package io.github.atunkodev.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.FileChange;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiffDialogTest {

    // --- deduplicateChanges ---

    @Test
    void deduplicateChangesEmptyListReturnsEmpty() {
        assertThat(DiffDialog.deduplicateChanges(List.of())).isEmpty();
    }

    @Test
    void deduplicateChangesNoDuplicatesPreservesOrder() {
        FileChange a = new FileChange(Path.of("A.java"), "old", "new");
        FileChange b = new FileChange(Path.of("B.java"), "old", "new");

        List<FileChange> result = DiffDialog.deduplicateChanges(List.of(a, b));

        assertThat(result).containsExactly(a, b);
    }

    @Test
    void deduplicateChangesDuplicatePathKeepsLast() {
        FileChange first = new FileChange(Path.of("A.java"), "v1", "v2");
        FileChange second = new FileChange(Path.of("A.java"), "v2", "v3");

        List<FileChange> result = DiffDialog.deduplicateChanges(List.of(first, second));

        assertThat(result).containsExactly(second);
    }

    @Test
    void deduplicateChangesMixedDuplicatesAndUnique() {
        FileChange a1 = new FileChange(Path.of("A.java"), "a1", "a2");
        FileChange b = new FileChange(Path.of("B.java"), "b1", "b2");
        FileChange a2 = new FileChange(Path.of("A.java"), "a2", "a3");

        List<FileChange> result = DiffDialog.deduplicateChanges(List.of(a1, b, a2));

        assertThat(result).containsExactly(a2, b);
    }

    // --- buildUnifiedDiff ---

    @Test
    void buildUnifiedDiffModifiedFileProducesDiff() {
        FileChange change = new FileChange(
                Path.of("src/Hello.java"),
                "public class Hello {\n    // old\n}\n",
                "public class Hello {\n    // new\n}\n");

        String diff = DiffDialog.buildUnifiedDiff(change);

        assertThat(diff).contains("--- a/src/Hello.java");
        assertThat(diff).contains("+++ b/src/Hello.java");
        assertThat(diff).contains("-    // old");
        assertThat(diff).contains("+    // new");
    }

    @Test
    void buildUnifiedDiffNewFileUsesDevNullAsSource() {
        FileChange change = new FileChange(Path.of("New.java"), null, "public class New {}");

        String diff = DiffDialog.buildUnifiedDiff(change);

        assertThat(diff).contains("--- /dev/null");
        assertThat(diff).contains("+++ b/New.java");
    }

    @Test
    void buildUnifiedDiffDeletedFileUsesDevNullAsTarget() {
        FileChange change = new FileChange(Path.of("Old.java"), "public class Old {}", null);

        String diff = DiffDialog.buildUnifiedDiff(change);

        assertThat(diff).contains("--- a/Old.java");
        assertThat(diff).contains("+++ /dev/null");
    }

    @Test
    void buildUnifiedDiffIdenticalContentReturnsEmpty() {
        FileChange change = new FileChange(Path.of("Same.java"), "same content", "same content");

        String diff = DiffDialog.buildUnifiedDiff(change);

        assertThat(diff).isEmpty();
    }
}

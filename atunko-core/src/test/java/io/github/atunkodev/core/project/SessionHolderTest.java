package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CORE_0004.4"})
class SessionHolderTest {

    @BeforeEach
    void reset() {
        SessionHolder.init(Path.of("."), null);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void getProjectDirDefaultsToCurrentDir() {
        assertThat(SessionHolder.getProjectDir()).isEqualTo(Path.of("."));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void getProjectInfoDefaultsToNull() {
        assertThat(SessionHolder.getProjectInfo()).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void initStoresProjectDir() {
        Path dir = Path.of("/some/project");
        SessionHolder.init(dir, null);
        assertThat(SessionHolder.getProjectDir()).isEqualTo(dir);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void initStoresProjectInfo() {
        Path dir = Path.of("/some/project");
        ProjectInfo info = new ProjectInfo(List.of(), List.of(dir));
        SessionHolder.init(dir, info);
        assertThat(SessionHolder.getProjectInfo()).isEqualTo(info);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void initCanBeCalledMultipleTimesUpdatesState() {
        SessionHolder.init(Path.of("/first"), null);
        Path second = Path.of("/second");
        SessionHolder.init(second, null);
        assertThat(SessionHolder.getProjectDir()).isEqualTo(second);
    }

    /** Records how often it was asked to scan, and can be told to fail a number of times first. */
    private static final class CountingScanner implements ProjectScanner {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger failuresLeft;
        private final CountDownLatch gate;

        CountingScanner(int failures, CountDownLatch gate) {
            this.failuresLeft = new AtomicInteger(failures);
            this.gate = gate;
        }

        @Override
        public ProjectInfo scan(Path projectDir) {
            calls.incrementAndGet();
            if (gate != null) {
                try {
                    gate.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (failuresLeft.getAndDecrement() > 0) {
                throw new IllegalStateException("scan boom");
            }
            return new ProjectInfo(List.of(), List.of(projectDir));
        }
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017"})
    void initLazyRecordsDirWithoutScanning() {
        CountingScanner scanner = new CountingScanner(0, null);
        Path dir = Path.of("/lazy/project");

        SessionHolder.initLazy(dir, scanner);

        assertThat(SessionHolder.getProjectDir()).isEqualTo(dir);
        assertThat(SessionHolder.getProjectEntries()).isEmpty();
        assertThat(SessionHolder.getProjectInfo()).isNull();
        assertThat(scanner.calls).hasValue(0);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017"})
    void ensureScannedPopulatesEntries() {
        CountingScanner scanner = new CountingScanner(0, null);
        Path dir = Path.of("/lazy/project");
        SessionHolder.initLazy(dir, scanner);

        SessionHolder.ensureScanned();

        assertThat(scanner.calls).hasValue(1);
        assertThat(SessionHolder.getProjectEntries()).hasSize(1);
        assertThat(SessionHolder.getProjectInfo()).isNotNull();
        assertThat(SessionHolder.getProjectDir()).isEqualTo(dir);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017.1"})
    void ensureScannedIsMemoized() {
        CountingScanner scanner = new CountingScanner(0, null);
        SessionHolder.initLazy(Path.of("/lazy/project"), scanner);

        SessionHolder.ensureScanned();
        SessionHolder.ensureScanned();
        SessionHolder.ensureScanned();

        assertThat(scanner.calls).hasValue(1);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017.1"})
    void ensureScannedScansOnceUnderConcurrentCallers() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        CountingScanner scanner = new CountingScanner(0, gate);
        SessionHolder.initLazy(Path.of("/lazy/project"), scanner);

        int threads = 8;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                SessionHolder.ensureScanned();
                done.countDown();
            });
            t.setDaemon(true);
            t.start();
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        gate.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(scanner.calls).hasValue(1);
        assertThat(SessionHolder.getProjectInfo()).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017.2"})
    void ensureScannedRethrowsAndRetriesOnFailure() {
        CountingScanner scanner = new CountingScanner(1, null);
        Path dir = Path.of("/lazy/project");
        SessionHolder.initLazy(dir, scanner);

        assertThatThrownBy(SessionHolder::ensureScanned)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scan boom");
        assertThat(SessionHolder.getProjectDir()).isEqualTo(dir);
        assertThat(SessionHolder.getProjectInfo()).isNull();

        SessionHolder.ensureScanned();

        assertThat(scanner.calls).hasValue(2);
        assertThat(SessionHolder.getProjectInfo()).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017.2"})
    void ensureScannedIsANoOpForEagerSessions() {
        Path dir = Path.of("/eager/project");
        ProjectInfo info = new ProjectInfo(List.of(), List.of(dir));
        SessionHolder.init(dir, info);

        SessionHolder.ensureScanned();

        assertThat(SessionHolder.getProjectInfo()).isEqualTo(info);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0017.2"})
    void eagerWorkspaceInitClearsAnyPendingLazyScan() {
        CountingScanner scanner = new CountingScanner(0, null);
        SessionHolder.initLazy(Path.of("/lazy/project"), scanner);

        Path root = Path.of("/workspace");
        Path project = Path.of("/workspace/a");
        SessionHolder.initWorkspace(root, List.of(new ProjectEntry(project, new ProjectInfo(List.of(), List.of()))));
        SessionHolder.ensureScanned();

        assertThat(scanner.calls).hasValue(0);
        assertThat(SessionHolder.getWorkspaceRoot()).isEqualTo(root);
        assertThat(SessionHolder.getProjectDir()).isEqualTo(project);
    }
}

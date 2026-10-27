package io.github.atunkodev.testing;

import io.github.atunkodev.daemon.DaemonRegistry;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Teardown for tests that spawn daemon JVMs into a {@code @TempDir} registry. */
public final class DaemonProcesses {

    private DaemonProcesses() {}

    /**
     * Stops every daemon registered in {@code registryDir} and waits for it to exit, so none is still running when
     * JUnit deletes the directory. Skips this JVM, which some tests register under a synthetic entry.
     */
    public static void stopAll(Path registryDir) {
        long self = ProcessHandle.current().pid();
        new DaemonRegistry(registryDir).list().forEach(entry -> {
            if (entry.pid() != self) {
                ProcessHandle.of(entry.pid()).ifPresent(DaemonProcesses::destroyAndWait);
            }
        });
    }

    private static void destroyAndWait(ProcessHandle handle) {
        handle.destroy();
        try {
            handle.onExit().get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            handle.destroyForcibly();
        } catch (ExecutionException e) {
            // The process is gone, which is all this needs.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

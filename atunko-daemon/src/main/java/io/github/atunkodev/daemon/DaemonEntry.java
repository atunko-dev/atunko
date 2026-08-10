package io.github.atunkodev.daemon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.file.Path;

/**
 * A registered daemon, as persisted in its registry file.
 *
 * @param projectRoot resolved (symlink-free, absolute) root the daemon serves — the registry key
 * @param port loopback port the daemon listens on
 * @param pid daemon process id, used to detect entries left behind by a crash
 * @param atunkoVersion version that started the daemon; a client of another version must not reuse it, since the
 *     cached LSTs were parsed by that version's OpenRewrite
 * @param token shared secret every request must present
 * @param lastUsedEpochMillis last time a client used this daemon, for least-recently-used eviction
 */
public record DaemonEntry(
        Path projectRoot, int port, long pid, String atunkoVersion, String token, long lastUsedEpochMillis) {

    /**
     * Whether the recorded process is still around. A dead pid means the entry is stale and must be discarded.
     *
     * <p>{@link JsonIgnore} because Jackson would otherwise persist this as a field and then reject it on read-back —
     * and a liveness answer frozen at write time is worthless anyway.
     */
    @JsonIgnore
    public boolean isProcessAlive() {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}

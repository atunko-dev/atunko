package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
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
    void getProjectDir_defaultsToCurrentDir() {
        assertThat(SessionHolder.getProjectDir()).isEqualTo(Path.of("."));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void getProjectInfo_defaultsToNull() {
        assertThat(SessionHolder.getProjectInfo()).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void init_storesProjectDir() {
        Path dir = Path.of("/some/project");
        SessionHolder.init(dir, null);
        assertThat(SessionHolder.getProjectDir()).isEqualTo(dir);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void init_storesProjectInfo() {
        Path dir = Path.of("/some/project");
        ProjectInfo info = new ProjectInfo(List.of(), List.of(dir));
        SessionHolder.init(dir, info);
        assertThat(SessionHolder.getProjectInfo()).isEqualTo(info);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.4"})
    void init_canBeCalledMultipleTimes_updatesState() {
        SessionHolder.init(Path.of("/first"), null);
        Path second = Path.of("/second");
        SessionHolder.init(second, null);
        assertThat(SessionHolder.getProjectDir()).isEqualTo(second);
    }
}

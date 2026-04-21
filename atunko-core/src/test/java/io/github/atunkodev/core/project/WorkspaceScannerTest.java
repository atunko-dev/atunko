package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CORE_0010"})
class WorkspaceScannerTest {

    private static Path fixture(String name) throws URISyntaxException {
        return Paths.get(WorkspaceScannerTest.class
                        .getClassLoader()
                        .getResource("workspaces/" + name)
                        .toURI())
                .toAbsolutePath()
                .normalize();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010"})
    void flatWorkspaceDiscoversAllThreeProjects() throws Exception {
        Path root = fixture("workspace-flat");
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(root);
        assertThat(candidates)
                .containsExactlyInAnyOrder(
                        root.resolve("service-auth"), root.resolve("service-payments"), root.resolve("service-ui"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010.1"})
    void skipsDirNamesInSkipList() throws Exception {
        Path root = fixture("workspace-flat");
        // build/ and target/ inside projects must not be discovered as projects themselves
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(root);
        assertThat(candidates).noneMatch(p -> {
            String name = p.getFileName().toString();
            return name.equals("build") || name.equals("target") || name.startsWith(".");
        });
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010.2"})
    void respectsAtunkoignoreMarker() throws Exception {
        Path root = fixture("workspace-atunkoignore");
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(root);
        assertThat(candidates).containsExactly(root.resolve("visible-project"));
        assertThat(candidates).noneMatch(p -> p.getFileName().toString().equals("hidden-project"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010.3"})
    void gradleMultiProjectRootDiscoveredOnce() throws Exception {
        Path root = fixture("workspace-gradle-multi");
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(root);
        // Root should appear; subprojects core/ and api/ must NOT be independently discovered
        assertThat(candidates).containsExactly(root);
        assertThat(candidates).noneMatch(p -> p.getFileName().toString().equals("core"));
        assertThat(candidates).noneMatch(p -> p.getFileName().toString().equals("api"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010.4"})
    void mavenMultiModuleRootDiscoveredOnce() throws Exception {
        Path root = fixture("workspace-maven-multi");
        List<Path> candidates = WorkspaceScanner.discoverProjectDirs(root);
        // Root should appear; module dirs auth/ and payments/ must NOT be independently discovered
        assertThat(candidates).containsExactly(root);
        assertThat(candidates).noneMatch(p -> p.getFileName().toString().equals("auth"));
        assertThat(candidates).noneMatch(p -> p.getFileName().toString().equals("payments"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0010.5"})
    void projectEntryPairsProjectDirWithInfo() {
        Path dir = Path.of("/some/project");
        ProjectInfo info = new ProjectInfo(List.of(), List.of());
        ProjectEntry entry = new ProjectEntry(dir, info);
        assertThat(entry.projectDir()).isEqualTo(dir);
        assertThat(entry.info()).isSameAs(info);
    }
}

package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;

public class GradleProjectScanner {

    @Requirements({"atunko:CORE_0004"})
    public ProjectInfo scan(Path projectDir) {
        Path absoluteDir = projectDir.toAbsolutePath().normalize();

        if (!Files.isDirectory(absoluteDir)) {
            throw new IllegalArgumentException("Directory does not exist: " + absoluteDir);
        }
        if (!Files.exists(absoluteDir.resolve("settings.gradle"))
                && !Files.exists(absoluteDir.resolve("settings.gradle.kts"))
                && !Files.exists(absoluteDir.resolve("build.gradle"))
                && !Files.exists(absoluteDir.resolve("build.gradle.kts"))) {
            throw new IllegalArgumentException("No Gradle build files found in " + absoluteDir);
        }

        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(absoluteDir.toFile());

        try (ProjectConnection connection = connector.connect()) {
            IdeaProject ideaProject = connection.model(IdeaProject.class).get();

            List<Path> classpath = new ArrayList<>();
            List<Path> sourceDirs = new ArrayList<>();
            List<Path> resourceDirs = new ArrayList<>();
            List<Path> testSourceDirs = new ArrayList<>();
            List<Path> testResourceDirs = new ArrayList<>();

            for (IdeaModule module : ideaProject.getModules()) {
                classpath.addAll(resolveClasspath(module));
                resolveDirectories(module, sourceDirs, resourceDirs, testSourceDirs, testResourceDirs);
            }

            return new ProjectInfo(classpath, sourceDirs, resourceDirs, testSourceDirs, testResourceDirs);
        }
    }

    private List<Path> resolveClasspath(IdeaModule module) {
        List<Path> classpath = new ArrayList<>();
        for (IdeaDependency dep : module.getDependencies()) {
            if (dep instanceof IdeaSingleEntryLibraryDependency libraryDep) {
                classpath.add(libraryDep.getFile().toPath());
            }
        }
        return classpath;
    }

    private void resolveDirectories(
            IdeaModule module,
            List<Path> sourceDirs,
            List<Path> resourceDirs,
            List<Path> testSourceDirs,
            List<Path> testResourceDirs) {
        for (IdeaContentRoot root : module.getContentRoots()) {
            for (IdeaSourceDirectory d : root.getSourceDirectories()) {
                sourceDirs.add(d.getDirectory().toPath());
            }
            for (IdeaSourceDirectory d : root.getTestDirectories()) {
                testSourceDirs.add(d.getDirectory().toPath());
            }
            for (IdeaSourceDirectory d : root.getResourceDirectories()) {
                resourceDirs.add(d.getDirectory().toPath());
            }
            for (IdeaSourceDirectory d : root.getTestResourceDirectories()) {
                testResourceDirs.add(d.getDirectory().toPath());
            }
        }
    }
}

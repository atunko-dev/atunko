package io.github.atunkodev.core.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything {@link ProjectSourceParser} needs to parse a project.
 *
 * @param classpath compile classpath entries, used to type-attribute Java sources
 * @param sourceDirs main source directories
 * @param resourceDirs main resource directories
 * @param testSourceDirs test source directories
 * @param testResourceDirs test resource directories
 * @param buildFiles build files that live outside the source/resource directories (for example the {@code pom.xml}
 *     of each Maven module); parsed with the build-system parser so build-model recipes can act
 */
public record ProjectInfo(
        List<Path> classpath,
        List<Path> sourceDirs,
        List<Path> resourceDirs,
        List<Path> testSourceDirs,
        List<Path> testResourceDirs,
        List<Path> buildFiles) {

    public ProjectInfo {
        classpath = List.copyOf(classpath);
        sourceDirs = List.copyOf(sourceDirs);
        resourceDirs = List.copyOf(resourceDirs);
        testSourceDirs = List.copyOf(testSourceDirs);
        testResourceDirs = List.copyOf(testResourceDirs);
        buildFiles = List.copyOf(buildFiles);
    }

    public ProjectInfo(List<Path> classpath, List<Path> sourceDirs) {
        this(classpath, sourceDirs, List.of(), List.of(), List.of(), List.of());
    }

    public ProjectInfo(
            List<Path> classpath,
            List<Path> sourceDirs,
            List<Path> resourceDirs,
            List<Path> testSourceDirs,
            List<Path> testResourceDirs) {
        this(classpath, sourceDirs, resourceDirs, testSourceDirs, testResourceDirs, List.of());
    }

    public List<Path> allSourceAndResourceDirs() {
        List<Path> all = new ArrayList<>();
        all.addAll(sourceDirs);
        all.addAll(resourceDirs);
        all.addAll(testSourceDirs);
        all.addAll(testResourceDirs);
        return List.copyOf(all);
    }
}

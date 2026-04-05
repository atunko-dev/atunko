package io.github.atunkodev.core.project;

import java.nio.file.Path;

/** Scans a project directory and returns resolved classpath and source directory information. */
public interface ProjectScanner {
    ProjectInfo scan(Path projectDir);
}

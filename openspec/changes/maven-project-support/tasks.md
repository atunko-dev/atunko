## 1. Test Fixture

- [x] 1.1 Create a minimal Maven test fixture project at `core/src/test/resources/fixtures/maven-project/` with a `pom.xml` that has at least one compile dependency

## 2. Tests (TDD — SVC_CORE_0005)

- [x] 2.1 Write `MavenProjectScannerTest.scan_returnsNonEmptyClasspath` — asserts classpath contains JARs
- [x] 2.2 Write `MavenProjectScannerTest.scan_returnsSourceDirectories` — asserts source dirs contain `src/main/java`
- [x] 2.3 Write `MavenProjectScannerTest.scan_nonExistentDirectory_throws` — asserts exception for invalid path

## 3. Implementation (CORE_0005)

- [x] 3.1 Implement `MavenProjectScanner.scan(Path)` — invoke `mvn dependency:build-classpath`, parse output file, detect source dirs, return `ProjectInfo`
- [x] 3.2 Add `@Requirements({"CORE_0005"})` annotation to the `scan` method
- [x] 3.3 Add `@SVCs({"SVC_CORE_0005"})` annotations to test methods

## 4. Verify

- [x] 4.1 Run `./gradlew :core:test` and confirm all tests pass
- [x] 4.2 Run `./gradlew build` and confirm full build passes

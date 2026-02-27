# reqstool Annotation Conventions

## Principle: As Close to the Implementation as Possible

Place `@Requirements` and `@SVCs` annotations **on the method or function that directly
implements or verifies the requirement** — not on the class or module.

This applies across languages. The exact granularity depends on what the language supports:

| Language | `@Requirements` placement | `@SVCs` placement |
|----------|---------------------------|-------------------|
| Java     | On the implementing method | On the test method |
| Python   | On the implementing function (decorator) | On the test function (decorator) |

## Why Method Level?

- **Precise traceability**: each annotation maps to the exact code that fulfills the requirement
- **Easier auditing**: reviewers can verify implementation by reading one method, not an entire class
- **Refactoring safety**: if a method moves to another class, the annotation moves with it

## Java Examples

### `@Requirements` — on the implementation method

```java
import io.github.reqstool.reqstool_java_annotations.annotations.Requirements;

public class RecipeDiscoveryEngine {

    @Requirements({"CORE_0001"})
    public List<Recipe> discoverRecipes(Path projectDir) {
        // implementation
    }

    @Requirements({"CORE_0005"})
    public ClasspathResult resolveClasspath(Path projectDir) {
        // implementation
    }
}
```

### `@SVCs` — on the test method

```java
import io.github.reqstool.reqstool_java_annotations.annotations.SVCs;

class RecipeDiscoveryEngineTest {

    @Test
    @SVCs({"SVC_CORE_0001"})
    void discoverRecipes_returnsAvailableRecipes() {
        // GIVEN a project with OpenRewrite on the classpath
        // WHEN discoverRecipes is called
        // THEN available recipes are returned
    }

    @Test
    @SVCs({"SVC_CORE_0005"})
    void resolveClasspath_handlesMavenProject() {
        // GIVEN a Maven project directory
        // WHEN resolveClasspath is called
        // THEN the classpath is resolved successfully
    }
}
```

### Avoid: class-level annotations

```java
// BAD — too coarse, hard to tell which method implements which requirement
@Requirements({"CORE_0001", "CORE_0005"})
public class RecipeDiscoveryEngine {
    // ...
}
```

package io.github.atunkodev.core.recipe;

import org.openrewrite.config.Environment;

/**
 * Lazy-initialized, thread-safe provider for the OpenRewrite {@link Environment}. The Environment is immutable once
 * built, so it is safe to cache and share across callers.
 */
public class EnvironmentProvider {

    private volatile Environment environment;

    public Environment get() {
        Environment result = environment;
        if (result == null) {
            synchronized (this) {
                result = environment;
                if (result == null) {
                    result = Environment.builder().scanRuntimeClasspath().build();
                    environment = result;
                }
            }
        }
        return result;
    }

    public void invalidate() {
        environment = null;
    }
}

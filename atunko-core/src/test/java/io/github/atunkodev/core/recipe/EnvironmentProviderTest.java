package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;

class EnvironmentProviderTest {

    @Test
    void get_returnsSameInstanceOnSubsequentCalls() {
        EnvironmentProvider provider = new EnvironmentProvider();

        Environment first = provider.get();
        Environment second = provider.get();

        assertThat(second).isSameAs(first);
    }

    @Test
    void invalidate_causesRebuildOnNextGet() {
        EnvironmentProvider provider = new EnvironmentProvider();

        Environment first = provider.get();
        provider.invalidate();
        Environment second = provider.get();

        assertThat(second).isNotSameAs(first);
    }
}

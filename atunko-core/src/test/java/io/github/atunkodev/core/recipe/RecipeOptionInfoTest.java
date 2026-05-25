package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CORE_0014.1"})
class RecipeOptionInfoTest {

    @Test
    void fieldsAreAccessible() {
        var opt = new RecipeOptionInfo(
                "targetVersion",
                "String",
                "Target Java version",
                "The Java version to migrate to",
                "17",
                List.of("11", "17", "21"),
                true);

        assertThat(opt.name()).isEqualTo("targetVersion");
        assertThat(opt.type()).isEqualTo("String");
        assertThat(opt.displayName()).isEqualTo("Target Java version");
        assertThat(opt.description()).isEqualTo("The Java version to migrate to");
        assertThat(opt.example()).isEqualTo("17");
        assertThat(opt.valid()).containsExactly("11", "17", "21");
        assertThat(opt.required()).isTrue();
    }

    @Test
    void nullableFieldsAcceptNull() {
        var opt = new RecipeOptionInfo("flag", "boolean", "Flag", "A flag", null, null, false);

        assertThat(opt.example()).isNull();
        assertThat(opt.valid()).isNull();
    }
}

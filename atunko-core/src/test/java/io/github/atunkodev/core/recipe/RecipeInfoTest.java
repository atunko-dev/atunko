package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;

@SVCs({"atunko:SVC_CORE_0001.2"})
class RecipeInfoTest {

    @Test
    void of_createsRecipeInfoWithExpectedFields() {
        RecipeInfo info = RecipeInfo.of("org.test.Foo", "Foo Recipe", "A test recipe", Set.of("java", "testing"));

        assertThat(info.name()).isEqualTo("org.test.Foo");
        assertThat(info.displayName()).isEqualTo("Foo Recipe");
        assertThat(info.description()).isEqualTo("A test recipe");
        assertThat(info.tags()).containsExactlyInAnyOrder("java", "testing");
        assertThat(info.recipeList()).isEmpty();
        assertThat(info.isComposite()).isFalse();
        assertThat(info.options()).isEmpty();
    }

    @Test
    void of_withRecipeList_createsCompositeRecipe() {
        RecipeInfo child = RecipeInfo.of("org.test.Child", "Child", "A child", Set.of());
        RecipeInfo composite = RecipeInfo.of("org.test.Parent", "Parent", "A parent", Set.of("java"), List.of(child));

        assertThat(composite.isComposite()).isTrue();
        assertThat(composite.recipeList()).hasSize(1);
        assertThat(composite.recipeList().getFirst().name()).isEqualTo("org.test.Child");
    }

    @Test
    void wrappedDescriptor_exposesOptions() {
        OptionDescriptor option = new OptionDescriptor(
                "targetVersion", "String", "Target version", "The Java version", "17", null, true, null);
        RecipeDescriptor desc = new RecipeDescriptor(
                "org.test.Recipe",
                "Test Recipe",
                null,
                "A recipe with options",
                Set.of("java"),
                null,
                List.of(option),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);
        RecipeInfo info = new RecipeInfo(desc, List.of());

        assertThat(info.options()).hasSize(1);
        OptionDescriptor opt = info.options().getFirst();
        assertThat(opt.getName()).isEqualTo("targetVersion");
        assertThat(opt.getType()).isEqualTo("String");
        assertThat(opt.isRequired()).isTrue();
        assertThat(opt.getDescription()).isEqualTo("The Java version");
        assertThat(opt.getExample()).isEqualTo("17");
    }

    @Test
    void discoveredRecipes_withOptions_exposeOptionMetadata() {
        RecipeDiscoveryService service = new RecipeDiscoveryService();
        List<RecipeInfo> recipes = service.discoverAll();

        List<RecipeInfo> withOptions =
                recipes.stream().filter(r -> !r.options().isEmpty()).toList();

        assertThat(withOptions).isNotEmpty();
        assertThat(withOptions)
                .allSatisfy(recipe -> assertThat(recipe.options()).allSatisfy(opt -> {
                    assertThat(opt.getName()).isNotBlank();
                    assertThat(opt.getType()).isNotBlank();
                }));
    }
}

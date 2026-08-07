package io.github.atunkodev.web.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.project.SourceCapability;
import io.github.atunkodev.core.recipe.RecipeApplicability;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.web.RecipeHolder;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies that the Web UI renders inapplicable recipes muted, badged and with the reason available. */
class RecipeBrowserViewApplicabilityTest {

    private static final RecipeInfo MAVEN_RECIPE = new RecipeInfo(
            "org.openrewrite.maven.ChangePropertyValue",
            "Change Maven property value",
            "Changes a property in the pom",
            Set.of("maven"));
    private static final RecipeInfo JAVA_RECIPE = new RecipeInfo(
            "org.openrewrite.java.RemoveUnusedImports",
            "Remove unused imports",
            "Removes unused imports",
            Set.of("java"));

    private static final Routes ROUTES = new Routes().autoDiscoverViews("io.github.atunkodev.web");

    private RecipeBrowserView setupView() {
        RecipeHolder.init(List.of(JAVA_RECIPE, MAVEN_RECIPE));
        MockVaadin.setup(ROUTES);
        return _get(RecipeBrowserView.class);
    }

    @BeforeEach
    void resetServices() {
        AppServices.init(null, null, null);
        AppServices.setSourceCapabilities(Set.of());
        SessionHolder.init(List.of(new ProjectEntry(Path.of("."), null)));
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
        AppServices.setSourceCapabilities(Set.of());
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0003"})
    void mavenRecipeIsInapplicableWhenNoMavenCapabilityIsAvailable() {
        RecipeBrowserView view = setupView();

        RecipeApplicability mavenApplicability = view.applicability(MAVEN_RECIPE);
        assertThat(mavenApplicability.applicable()).isFalse();
        assertThat(mavenApplicability.badgeLabel()).isEqualTo("needs Maven");
        assertThat(view.applicability(JAVA_RECIPE).applicable()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0003"})
    void treeCellForAnInapplicableRecipeIsMutedAndBadgedWithTheReasonAsTooltip() {
        RecipeBrowserView view = setupView();

        Component cell = view.renderRecipeName(new TreeNode(MAVEN_RECIPE, MAVEN_RECIPE.name()));

        assertThat(cell).isInstanceOf(HorizontalLayout.class);
        HorizontalLayout row = (HorizontalLayout) cell;
        List<Span> spans = row.getChildren()
                .filter(Span.class::isInstance)
                .map(Span.class::cast)
                .toList();
        assertThat(spans).hasSize(2);
        assertThat(spans.get(0).getText()).isEqualTo("Change Maven property value");
        assertThat(spans.get(0).getStyle().get("color")).isEqualTo("var(--lumo-disabled-text-color)");
        assertThat(spans.get(1).getText()).isEqualTo("needs Maven");
        assertThat(spans.get(1).getElement().getThemeList()).contains("badge");
        assertThat(spans.get(1).getTitle())
                .hasValueSatisfying(t -> assertThat(t).contains("Requires Maven build model"));
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0003"})
    void treeCellForAnApplicableRecipeIsAPlainUnstyledName() {
        RecipeBrowserView view = setupView();

        Component cell = view.renderRecipeName(new TreeNode(JAVA_RECIPE, JAVA_RECIPE.name()));

        assertThat(cell).isInstanceOf(Span.class);
        Span name = (Span) cell;
        assertThat(name.getText()).isEqualTo("Remove unused imports");
        assertThat(name.getStyle().get("color")).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0003"})
    void badgeDisappearsOnceTheMavenCapabilityIsAvailable() {
        RecipeBrowserView view = setupView();
        AppServices.setSourceCapabilities(Set.of(SourceCapability.JAVA, SourceCapability.MAVEN));

        assertThat(view.applicability(MAVEN_RECIPE).applicable()).isTrue();
        assertThat(view.renderRecipeName(new TreeNode(MAVEN_RECIPE, MAVEN_RECIPE.name())))
                .isInstanceOf(Span.class);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0003"})
    void detailPanelShowsTheApplicabilityReasonOnlyForInapplicableRecipes() {
        RecipeBrowserView view = setupView();

        view.selectForDetail(MAVEN_RECIPE);
        assertThat(spanTexts())
                .anyMatch(t -> t.startsWith("Applicability: ") && t.contains("Requires Maven build model"));

        view.selectForDetail(JAVA_RECIPE);
        assertThat(spanTexts()).noneMatch(t -> t.startsWith("Applicability: "));
    }

    private static List<String> spanTexts() {
        return _find(Span.class).stream().map(Span::getText).toList();
    }
}

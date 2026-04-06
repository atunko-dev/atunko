package io.github.atunkodev.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

/**
 * Playwright e2e tests for features that require real browser rendering: diff2html JavaScript
 * rendering, side-by-side diff layout.
 */
class DiffDialogE2eTest extends PlaywrightTestBase {

    @Test
    void pageLoadsRecipeBrowserVisible() {
        navigateTo("/");
        assertThat(page).hasTitle("atunko");
        assertThat(page.locator("vaadin-app-layout")).isVisible();
    }

    @Test
    void dryRunRendersDiff2htmlSideBySide() {
        navigateTo("/");

        // Select the leaf recipe in the tree grid
        page.locator("vaadin-grid-tree-toggle").first().click();

        // Click Dry Run button
        page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Dry Run"))
                .click();

        // RunOrderDialog opens — click Confirm
        page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Confirm"))
                .click();

        // Wait for the DiffDialog to appear with diff2html content
        Locator diffWrapper = page.locator(".d2h-wrapper");
        diffWrapper.first().waitFor();

        // Verify diff2html rendered with side-by-side layout
        assertThat(diffWrapper.first()).isVisible();
        assertThat(page.locator(".d2h-file-wrapper").first()).isVisible();

        // Verify the diff contains our test file
        assertThat(page.locator(".d2h-file-header").first()).containsText("Hello.java");

        // Verify side-by-side columns are present
        assertThat(page.locator(".d2h-file-side-diff").first()).isVisible();
    }
}

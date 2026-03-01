package io.github.atunkodev.tui;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.tui.view.BrowserView;
import io.github.atunkodev.tui.view.DetailView;
import io.github.atunkodev.tui.view.ExecutionResultsView;
import io.github.atunkodev.tui.view.TagBrowserView;
import io.github.reqstool.annotations.Requirements;

@Requirements({"CLI_0001"})
public class AtunkoTui extends ToolkitApp {

    private final TuiController controller;

    public AtunkoTui(TuiController controller) {
        this.controller = controller;
    }

    @Override
    protected Element render() {
        return switch (controller.currentScreen()) {
            case BROWSER -> BrowserView.render(controller, this);
            case DETAIL -> DetailView.render(controller);
            case TAG_BROWSER -> TagBrowserView.render(controller);
            case EXECUTION_RESULTS -> ExecutionResultsView.render(controller);
            case RUN_CONFIG -> BrowserView.render(controller, this);
        };
    }

    public void requestQuit() {
        quit();
    }
}

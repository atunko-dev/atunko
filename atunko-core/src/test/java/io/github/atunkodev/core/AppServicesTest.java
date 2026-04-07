package io.github.atunkodev.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_WEB_0001.12"})
class AppServicesTest {

    @BeforeEach
    void reset() {
        AppServices.init(null, null, null);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.12"})
    void initStoresEngine() {
        RecipeExecutionEngine engine = new RecipeExecutionEngine(null);
        AppServices.init(engine, null, null);
        assertThat(AppServices.getEngine()).isSameAs(engine);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.12"})
    void initStoresSourceParser() {
        ProjectSourceParser parser = new ProjectSourceParser();
        AppServices.init(null, parser, null);
        assertThat(AppServices.getSourceParser()).isSameAs(parser);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.12"})
    void initStoresChangeApplier() {
        ChangeApplier applier = new ChangeApplier();
        AppServices.init(null, null, applier);
        assertThat(AppServices.getChangeApplier()).isSameAs(applier);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.12"})
    void initMultipleCallsOverwritesPreviousValues() {
        RecipeExecutionEngine engine1 = new RecipeExecutionEngine(null);
        RecipeExecutionEngine engine2 = new RecipeExecutionEngine(null);
        AppServices.init(engine1, null, null);
        AppServices.init(engine2, null, null);
        assertThat(AppServices.getEngine()).isSameAs(engine2);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.12"})
    void beforeInitAllGettersReturnNull() {
        assertThat(AppServices.getEngine()).isNull();
        assertThat(AppServices.getSourceParser()).isNull();
        assertThat(AppServices.getChangeApplier()).isNull();
    }
}

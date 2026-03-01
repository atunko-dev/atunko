package io.github.atunkodev.cli;

import io.github.atunkodev.core.config.RunConfigService;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.JavaSourceParser;
import io.github.atunkodev.core.recipe.EnvironmentProvider;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.atunkodev.tui.TuiCommand;
import picocli.CommandLine;

/**
 * Picocli factory that shares a single {@link EnvironmentProvider} across all services, avoiding redundant Environment
 * construction.
 */
public class ServiceFactory implements CommandLine.IFactory {

    private final EnvironmentProvider environmentProvider = new EnvironmentProvider();
    private final RecipeDiscoveryService discoveryService = new RecipeDiscoveryService(environmentProvider);
    private final CommandLine.IFactory defaultFactory = CommandLine.defaultFactory();

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        if (cls == ListCommand.class) {
            return cls.cast(new ListCommand(discoveryService));
        }
        if (cls == SearchCommand.class) {
            return cls.cast(new SearchCommand(discoveryService));
        }
        if (cls == RunCommand.class) {
            return cls.cast(new RunCommand(
                    new RecipeExecutionEngine(environmentProvider), new JavaSourceParser(), new ChangeApplier()));
        }
        if (cls == TuiCommand.class) {
            return cls.cast(new TuiCommand(discoveryService, new RunConfigService()));
        }
        return defaultFactory.create(cls);
    }
}

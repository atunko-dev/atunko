package io.github.atunkodev.cli;

import io.github.atunkodev.core.config.ConfigExportService;
import io.github.atunkodev.core.config.ConfigExportService.ExportMode;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.config.RunConfigService;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "config",
        description = "Manage run configurations",
        mixinStandardHelpOptions = true,
        subcommands = {ConfigExportCommand.ConfigExportSubcommand.class})
public class ConfigExportCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(
            name = "export",
            description = "Export a run configuration to Maven/Gradle plugin format",
            mixinStandardHelpOptions = true)
    public static class ConfigExportSubcommand implements Runnable {

        @Option(
                names = {"-f", "--file"},
                required = true,
                description = "Path to the .atunko.yml run configuration file")
        private Path configFile;

        @Option(names = "--gradle", description = "Export to Gradle format")
        private boolean gradle;

        @Option(names = "--maven", description = "Export to Maven format")
        private boolean maven;

        @Option(names = "--full", description = "Emit a full standalone build file instead of a plugin snippet")
        private boolean full;

        @Spec
        private CommandSpec spec;

        private final RunConfigService configService;
        private final ConfigExportService exportService;

        public ConfigExportSubcommand() {
            this(new RunConfigService(), new ConfigExportService());
        }

        public ConfigExportSubcommand(RunConfigService configService, ConfigExportService exportService) {
            this.configService = configService;
            this.exportService = exportService;
        }

        @Override
        @Requirements({"atunko:CORE_0009"})
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            PrintWriter err = spec.commandLine().getErr();

            if (!gradle && !maven) {
                err.println("Error: specify either --gradle or --maven");
                throw new picocli.CommandLine.ExecutionException(
                        spec.commandLine(), "Must specify --gradle or --maven");
            }

            if (gradle && maven) {
                err.println("Error: specify only one of --gradle or --maven");
                throw new picocli.CommandLine.ExecutionException(
                        spec.commandLine(), "Cannot specify both --gradle and --maven");
            }

            try {
                RunConfig config = configService.load(configFile);

                ExportMode mode = full ? ExportMode.FULL : ExportMode.MINIMAL;
                String output;
                if (gradle) {
                    output = exportService.exportToGradle(config, mode);
                } else {
                    output = exportService.exportToMaven(config, mode);
                }

                out.print(output);
            } catch (Exception e) {
                err.println("Error exporting config: " + e.getMessage());
                throw new picocli.CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
            }
        }
    }
}

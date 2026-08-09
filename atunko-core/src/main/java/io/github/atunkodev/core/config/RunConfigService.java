package io.github.atunkodev.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Path;

public class RunConfigService {

    private final ObjectMapper yamlMapper = YamlMappers.configMapper();

    @Requirements({"atunko:CORE_0007"})
    public void save(RunConfig config, Path file) throws IOException {
        yamlMapper.writeValue(file.toFile(), config);
    }

    @Requirements({"atunko:CORE_0008"})
    public RunConfig load(Path file) throws IOException {
        return yamlMapper.readValue(file.toFile(), RunConfig.class);
    }
}

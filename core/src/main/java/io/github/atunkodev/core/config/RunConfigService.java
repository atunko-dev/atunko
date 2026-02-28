package io.github.atunkodev.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Path;

public class RunConfigService {

    private final ObjectMapper yamlMapper;

    public RunConfigService() {
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        this.yamlMapper = new ObjectMapper(yamlFactory).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Requirements({"CORE_0007"})
    public void save(RunConfig config, Path file) throws IOException {
        yamlMapper.writeValue(file.toFile(), config);
    }

    @Requirements({"CORE_0008"})
    public RunConfig load(Path file) throws IOException {
        return yamlMapper.readValue(file.toFile(), RunConfig.class);
    }
}

package io.github.atunkodev.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * The one YAML {@link ObjectMapper} configuration for atunko's config files (run configs, favorites, recents), so
 * every file is written with identical YAML conventions and a mapper change happens in exactly one place.
 */
public final class YamlMappers {

    private YamlMappers() {}

    public static ObjectMapper configMapper() {
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        return new ObjectMapper(yamlFactory).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
}

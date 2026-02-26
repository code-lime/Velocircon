package org.lime.velocircon.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader<T> {
    private final Path file;
    private final Class<T> clazz;
    private final EnvLoader envLoader;
    private final YamlConfigurationLoader loader;
    private final ObjectMapper<T> mapper;

    private ConfigLoader(
            Path file,
            EnvLoader envLoader,
            Class<T> clazz,
            T defaultValue)
            throws IOException {
        this.file = file;
        this.clazz = clazz;
        this.envLoader = envLoader;
        this.loader = YamlConfigurationLoader.builder()
                .path(file)
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        this.mapper = ObjectMapper.factory()
                .get(clazz);

        if (!this.file.toFile().exists()) {
            Files.createDirectories(this.file.getParent());
            save(defaultValue);
        } else {
            update(defaultValue);
        }
    }
    private ConfigLoader(Path folder, String name, EnvLoader envLoader, Class<T> clazz, T defaultValue) throws IOException {
        this(folder.resolve(name + ".yml"), envLoader, clazz, defaultValue);
    }

    private void update(T defaultValue) throws IOException {
        ConfigurationNode node = this.loader.createNode();
        this.mapper.save(defaultValue, node);
        ConfigurationNode updated = this.loader.load();
        updated.mergeFrom(node);
        this.loader.save(updated);
    }

    public void save(T value) throws IOException {
        ConfigurationNode node = this.loader.createNode();
        this.mapper.save(value, node);
        this.loader.save(node);
    }
    public T load() throws IOException {
        ConfigurationNode node = this.loader.load();
        var resultNode = this.envLoader
                .loadOptional()
                .map(v -> v.mergeFrom(node))
                .orElse(node);
        return this.mapper.load(resultNode);
    }

    public Class<T> clazz() {
        return clazz;
    }
    public Path file() {
        return file;
    }

    public static <T>ConfigLoader<T> create(Path file, EnvLoader envLoader, Class<T> clazz, T defaultValue) throws IOException {
        return new ConfigLoader<>(file, envLoader, clazz, defaultValue);
    }
    public static <T>ConfigLoader<T> create(Path folder, String name, EnvLoader envLoader, Class<T> clazz, T defaultValue) throws IOException {
        return new ConfigLoader<>(folder, name, envLoader, clazz, defaultValue);
    }

    public static <T>ConfigLoader<T> create(Path file, EnvLoader envLoader, T defaultValue) throws IOException {
        return create(file, envLoader, (Class<T>)defaultValue.getClass(), defaultValue);
    }
    public static <T>ConfigLoader<T> create(Path folder, EnvLoader envLoader, String name, T defaultValue) throws IOException {
        return create(folder, name, envLoader, (Class<T>)defaultValue.getClass(), defaultValue);
    }
}

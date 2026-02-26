package org.lime.velocircon.config;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnvLoader {
    private final String separator;

    private final String prefixWithSeparator;
    private final int prefixWithSeparatorLength;

    private final Converter<String, String> nameConverter;

    public EnvLoader(String prefix, String separator, CaseFormat envCaseFormat, CaseFormat fieldCaseFormat) {
        this.nameConverter = envCaseFormat.converterTo(fieldCaseFormat);

        this.separator = separator;
        this.prefixWithSeparator = prefix + separator;
        this.prefixWithSeparatorLength = prefixWithSeparator.length();
    }
    public Optional<ConfigurationNode> loadOptional() {
        ConfigurationNode root = BasicConfigurationNode.root();
        AtomicBoolean hasValue = new AtomicBoolean(false);
        System.getenv()
                .forEach((key,value) -> {
                    if (!key.startsWith(prefixWithSeparator))
                        return;

                    String path = key.substring(prefixWithSeparatorLength);
                    String[] parts = path.split(separator);

                    ConfigurationNode node = root;

                    for (String part : parts)
                        node = node.node(nameConverter.convert(part));

                    try {
                        node.set(value);
                    } catch (SerializationException e) {
                        throw new RuntimeException("Error load " + key + " environment", e);
                    }

                    hasValue.set(true);
                });
        return hasValue.get() ? Optional.of(root) : Optional.empty();
    }
}

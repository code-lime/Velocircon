package org.lime.velocircon;

import com.google.common.collect.Iterables;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.group.ChannelGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.lime.velocircon.server.RconBinder;
import org.lime.velocircon.server.RconServer;
import org.lime.velocircon.utils.NettyFutureUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RconService
        implements RconServer {
    private static final long FLUSH_MILLISECONDS = 200L;

    private final Object plugin;
    private final ProxyServer server;
    private final ConfigLoader<RconConfig> configLoader;
    private final RconBinder binder;
    private final Logger logger;
    private @Nullable ChannelGroup channels;

    private String password;

    public RconService(Object plugin, ProxyServer server, ConfigLoader<RconConfig> configLoader, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.configLoader = configLoader;
        this.logger = logger;
        this.binder = new RconBinder(this, logger);
    }

    public CompletableFuture<Void> reloadConfig() throws IOException {
        RconConfig config = configLoader.load();
        InetSocketAddress address = new InetSocketAddress(config.host(), config.port());

        return disable()
                .handle((disabled, e) -> {
                    if (!config.enable())
                        return CompletableFuture.completedFuture((Void)null);
                    this.password = config.password();
                    return this.binder.bind(address, config)
                            .handle((channel, ex) -> {
                                if (channel != null)
                                    this.channels = channel;
                                return (Void)null;
                            });
                })
                .thenCompose(v -> v);
    }

    public CompletableFuture<Void> enable() throws IOException {
        return reloadConfig();
    }
    public CompletableFuture<Void> disable() {
        this.password = null;
        return Optional.ofNullable(this.channels)
                .map(v -> {
                    logger.info("Shutting down RCON server...");
                    return v.close();
                })
                .map(NettyFutureUtils::toCompletableFuture)
                .orElseGet(() -> CompletableFuture.completedFuture(null))
                .whenComplete((v, ex) -> {
                    if (ex != null)
                        logger.error("Error RCON shutting down", ex);
                    this.channels = null;
                });
    }

    @Override
    public String password() {
        return password;
    }
    @Override
    public CompletableFuture<Component> execute(String command) {
        RconCommandSource source = new RconCommandSource(this.plugin, this.server.getScheduler(), FLUSH_MILLISECONDS);
        return this.server
                .getCommandManager()
                .executeAsync(source, command)
                .handleAsync((state, ex) -> source
                        .outputAsync()
                        .handleAsync((output, exx) -> {
                            Iterable<Component> lines = Objects.requireNonNullElseGet(output, Collections::emptyList);
                            if (ex != null)
                                lines = Iterables.concat(lines, Collections.singleton(Component.text("Internal server error").color(NamedTextColor.RED)));
                            else if (!state)
                                lines = Iterables.concat(Collections.singleton(Component.text("No such command").color(NamedTextColor.RED)), lines);
                            return Component.join(JoinConfiguration.newlines(), lines);
                        }))
                .thenCompose(v -> v);
    }
}

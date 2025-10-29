package org.lime.velocircon;

import com.google.common.collect.Iterables;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.group.ChannelGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.Nullable;
import org.lime.velocircon.permissions.PermissionFactory;
import org.lime.velocircon.server.RconBinder;
import org.lime.velocircon.server.RconServer;
import org.lime.velocircon.utils.NettyFutureUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RconService
        implements RconServer {
    private static final long FLUSH_MILLISECONDS = 300L;
    private static final int FLUSH_WAIT_COUNT = 3;

    private final Object plugin;
    private final ProxyServer server;
    private final ConfigLoader<RconConfig> configLoader;
    private final RconBinder binder;
    private final Logger logger;
    private final ComponentLogger componentLogger;
    private Collection<PermissionFactory> permissionFactories = Collections.emptyList();
    private @Nullable ChannelGroup channels;

    private String password;
    private boolean consoleOutput;

    public RconService(
        Object plugin,
        ProxyServer server,
        ConfigLoader<RconConfig> configLoader,
        Logger logger,
        ComponentLogger componentLogger) {
        this.plugin = plugin;
        this.server = server;
        this.configLoader = configLoader;
        this.logger = logger;
        this.componentLogger = componentLogger;
        this.binder = new RconBinder(this, logger);
    }

    public CompletableFuture<Void> reloadConfig() throws IOException {
        RconConfig config = configLoader.load();
        InetSocketAddress address = new InetSocketAddress(config.host, config.port);

        return disable()
                .handle((disabled, e) -> {
                    if (!config.enable)
                        return CompletableFuture.completedFuture((Void)null);
                    this.permissionFactories = PermissionFactory.load(config, logger);
                    this.password = config.password;
                    this.consoleOutput = config.consoleOutput;
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
        this.permissionFactories = Collections.emptyList();
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
        RconCommandSource source = new RconCommandSource(
                command,
                this.plugin,
                this.server.getScheduler(),
                this.permissionFactories,
                FLUSH_MILLISECONDS,
                FLUSH_WAIT_COUNT,
                this.consoleOutput,
                this.componentLogger);
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
                            Component result = Component.join(JoinConfiguration.newlines(), lines);
                            return GlobalTranslator.render(result, Locale.getDefault());
                        }))
                .thenCompose(v -> v);
    }
}

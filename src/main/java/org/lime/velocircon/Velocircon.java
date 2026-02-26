package org.lime.velocircon;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.velocity.Metrics;
import org.lime.velocircon.config.ConfigLoader;
import org.lime.velocircon.config.RconConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = BuildConstants.ID,
        name = BuildConstants.NAME,
        version = BuildConstants.VERSION,
        description = "Enable RCON Protocol support on Velocity",
        authors = "Lime",
        dependencies = {
                @Dependency(id = "luckperms", optional = true),
        }
)
public class Velocircon {
    public final ProxyServer proxy;
    public final Logger logger;
    public final Path dataFolder;
    private final Metrics.Factory metricsFactory;

    public final RconService rconService;

    @Inject
    public Velocircon(
            ProxyServer proxy,
            Logger logger,
            ComponentLogger componentLogger,
            @DataDirectory Path dataFolder,
            Metrics.Factory metricsFactory) throws IOException {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.metricsFactory = metricsFactory;
        this.rconService = new RconService(
            this,
            proxy,
            ConfigLoader.create(dataFolder, "rcon", new RconConfig()),
            logger,
            componentLogger);
    }

    @Subscribe
    public EventTask onInitialize(ProxyInitializeEvent event) throws IOException {
        this.metricsFactory.make(this, BuildConstants.BSTATS_ID);
        Commands.register(this, this.proxy, this.rconService);
        return EventTask.resumeWhenComplete(this.rconService.enable());
    }
    @Subscribe
    public EventTask onShutdown(ProxyShutdownEvent event) {
        return EventTask.resumeWhenComplete(this.rconService.disable());
    }
}

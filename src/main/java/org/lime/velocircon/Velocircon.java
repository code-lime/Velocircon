package org.lime.velocircon;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = BuildConstants.ID,
        name = BuildConstants.NAME,
        version = BuildConstants.VERSION,
        description = "Enable RCON Protocol support on Velocity",
        authors = "Lime"
)
public class Velocircon {
    public static Velocircon instance;

    public final ProxyServer proxy;
    public final Logger logger;
    public final Path dataFolder;

    public final RconService rconService;

    @Inject
    public Velocircon(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) throws IOException {
        Velocircon.instance = this;

        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;

        this.rconService = new RconService(proxy, ConfigLoader.create(dataFolder, "rcon", RconConfig.DEFAULT), logger);
    }

    @Subscribe
    public EventTask onInitialize(ProxyInitializeEvent event) throws IOException {
        Commands.register();
        return EventTask.resumeWhenComplete(this.rconService.enable());
    }
    @Subscribe
    public EventTask onShutdown(ProxyShutdownEvent event) {
        return EventTask.resumeWhenComplete(this.rconService.disable());
    }
}

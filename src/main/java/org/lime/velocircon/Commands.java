package org.lime.velocircon;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;

public class Commands {
    public static void register(Object plugin, ProxyServer proxy, RconService rconService) {
        CommandManager commandManager = proxy.getCommandManager();
        List<LiteralArgumentBuilder<CommandSource>> commands = List.of(createCommand(rconService));
        commands.forEach(command -> {
            CommandMeta meta = commandManager
                    .metaBuilder(command.getLiteral())
                    .plugin(plugin)
                    .build();
            commandManager.register(meta, new BrigadierCommand(command.build()));
        });
    }
    private static LiteralArgumentBuilder<CommandSource> createCommand(RconService rconService) {
        return BrigadierCommand.literalArgumentBuilder("velocircon")
                .then(BrigadierCommand.literalArgumentBuilder("reload")
                        .requires(source -> source.hasPermission("velocircon.reload"))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            try {
                                rconService.reloadConfig().get();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            source.sendMessage(Component.text("Config reloaded"));
                            return Command.SINGLE_SUCCESS;
                        }));
    }
}

package org.lime.velocircon;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;

import java.util.List;

public class Commands {
    public static void register() {
        CommandManager commandManager = Velocircon.instance.proxy.getCommandManager();
        List<LiteralArgumentBuilder<CommandSource>> commands = List.of(createCommand());
        commands.forEach(command -> {
            CommandMeta meta = commandManager
                    .metaBuilder(command.getLiteral())
                    .plugin(Velocircon.instance)
                    .build();
            commandManager.register(meta, new BrigadierCommand(command.build()));
        });
    }
    private static LiteralArgumentBuilder<CommandSource> createCommand() {
        return BrigadierCommand.literalArgumentBuilder("velocircon")
                .then(BrigadierCommand.literalArgumentBuilder("reload")
                        .requires(source -> source.hasPermission("velocircon.reload"))
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            try {
                                Velocircon.instance.rconService
                                        .reloadConfig()
                                        .get();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            source.sendMessage(Component.text("Config reloaded"));
                            return Command.SINGLE_SUCCESS;
                        }));
    }
}

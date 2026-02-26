package org.lime.velocircon.sources;

import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

public class RootRconCommandSource
        extends BaseRconCommandSource
        implements ConsoleCommandSource {
    public RootRconCommandSource(
            String command,
            Object plugin,
            Scheduler scheduler,
            long flushMs,
            int flushWaitCount,
            boolean consoleOutput,
            ComponentLogger componentLogger) {
        super(command, plugin, scheduler, flushMs, flushWaitCount, consoleOutput, componentLogger);
    }

    @Override
    public @NotNull Tristate getPermissionValue(@NotNull String permission) {
        return Tristate.TRUE;
    }
}
